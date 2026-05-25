package com.example.tvserver

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.WindowManager
import android.accessibilityservice.AccessibilityService
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.tvserver.theme.TVServerTheme
import com.example.tvserver.ui.components.GlassmorphicCursor
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CursorOverlayService : Service(), Choreographer.FrameCallback {

    companion object {
        var isAdbMode: Boolean = false
        var cursorSize: Int = 32
        var cursorOpacity: Float = 0.6f
        var isCursorEnabled: Boolean = true
        var sensitivity: Float = 1.0f
        var acceleration: Float = 0.08f
        var momentumFriction: Float = 0.9f
    }

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var remoteService: RemoteService? = null
    private var isBound = false

    private var currentX = 960.0f
    private var currentY = 540.0f
    private var velocityX = 0.0f
    private var velocityY = 0.0f
    private val lerpAlpha = 0.25f

    private val cursorXState = mutableFloatStateOf(currentX)
    private val cursorYState = mutableFloatStateOf(currentY)
    private val cursorSizeState = mutableIntStateOf(cursorSize)
    private val cursorOpacityState = mutableFloatStateOf(cursorOpacity)
    private val cursorPressedState = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? RemoteService.LocalBinder
            remoteService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteService = null
            isBound = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, RemoteService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        val priorPositionCallback = RemoteServiceCallbacks.onPositionUpdated
        RemoteServiceCallbacks.onPositionUpdated = { dx, dy ->
            processRawMotion(dx, dy)
            priorPositionCallback?.invoke(dx, dy)
        }

        RemoteServiceCallbacks.onClickReceived = { isDown ->
            cursorPressedState.value = isDown
            if (isDown) {
                triggerClick()
            }
        }

        RemoteServiceCallbacks.onKeyReceived = { key ->
            triggerSystemKey(key)
        }

        RemoteServiceCallbacks.onBrightnessChanged = { value ->
            setTvBrightness(value)
        }

        RemoteServiceCallbacks.onVolumeChanged = { value ->
            setTvVolume(value)
        }

        RemoteServiceCallbacks.onPictureModeChanged = { mode ->
            setTvPictureMode(mode)
        }

        RemoteServiceCallbacks.onLaunchSetting = { value ->
            try {
                if (value == "TV_DASHBOARD") {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(launchIntent)
                        Log.i("CursorOverlayService", "Successfully launched TV Remote Dashboard")
                    }
                } else if (value == "PICTURE") {
                    val dispIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(dispIntent)
                } else if (value == "SOUND" || value == "AUDIO") {
                    val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(soundIntent)
                }
            } catch (e: Exception) {
                Log.e("CursorOverlayService", "Failed to launch overlay setting $value", e)
            }
        }

        setupSystemOverlay()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        RemoteServiceCallbacks.onPositionUpdated = null
        RemoteServiceCallbacks.onClickReceived = null
        RemoteServiceCallbacks.onKeyReceived = null
        RemoteServiceCallbacks.onBrightnessChanged = null
        RemoteServiceCallbacks.onVolumeChanged = null
        RemoteServiceCallbacks.onPictureModeChanged = null
        RemoteServiceCallbacks.onLaunchSetting = null

        Choreographer.getInstance().removeFrameCallback(this)
        composeView?.let { view ->
            windowManager?.removeView(view)
        }
        super.onDestroy()
    }

    private fun processRawMotion(dx: Float, dy: Float) {
        val rawDx = sensitivity * dx
        val rawDy = sensitivity * dy
        val magnitude = sqrt((rawDx * rawDx) + (rawDy * rawDy))
        val accelerationMultiplier = (acceleration * magnitude) + 1.0f
        velocityX += rawDx * accelerationMultiplier
        velocityY += rawDy * accelerationMultiplier
    }

    override fun doFrame(frameTimeNanos: Long) {
        remoteService?.let { service ->
            service.targetX += velocityX
            service.targetY += velocityY
            velocityX *= momentumFriction
            velocityY *= momentumFriction

            if (sqrt((velocityX * velocityX) + (velocityY * velocityY)) < 0.1f) {
                velocityX = 0.0f
                velocityY = 0.0f
            }

            currentX += lerpAlpha * (service.targetX - currentX)
            currentY += lerpAlpha * (service.targetY - currentY)

            val displayMetrics = resources.displayMetrics
            val w = displayMetrics.widthPixels.toFloat()
            val h = displayMetrics.heightPixels.toFloat()

            if (currentX < 0f) currentX = 0f
            if (currentY < 0f) currentY = 0f
            if (currentX > w) currentX = w
            if (currentY > h) currentY = h

            service.targetX = currentX
            service.targetY = currentY

            if (isCursorEnabled) {
                GtvAccessibilityService.getSharedInstance()?.updateHoverTarget(currentX, currentY)
            }

            cursorXState.floatValue = currentX
            cursorYState.floatValue = currentY
            cursorSizeState.intValue = cursorSize
            cursorOpacityState.floatValue = cursorOpacity
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun setupSystemOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                format = android.graphics.PixelFormat.TRANSLUCENT
                gravity = android.view.Gravity.TOP or android.view.Gravity.LEFT
            }

            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.start()

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            val viewModelStoreOwner = object : ViewModelStoreOwner {
                private val store = ViewModelStore()
                override val viewModelStore: ViewModelStore
                    get() = store
            }
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            setContent {
                TVServerTheme {
                    val posX by cursorXState
                    val posY by cursorYState
                    val size by cursorSizeState
                    val opacity by cursorOpacityState
                    val isPressed by cursorPressedState

                    if (opacity > 0.05f && isCursorEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (posX - size / 2).roundToInt(),
                                            (posY - size / 2).roundToInt()
                                        )
                                    }
                            ) {
                                GlassmorphicCursor(
                                    sizeDp = size,
                                    cursorColor = Color.White,
                                    opacity = opacity,
                                    isPressed = isPressed
                                )
                            }
                        }
                    }
                }
            }

            windowManager?.addView(this, layoutParams)
        }
    }

    private fun triggerClick() {
        if (!isCursorEnabled) {
            Log.d("CursorOverlayService", "Click ignored because cursor is disabled.")
            return
        }
        if (isAdbMode) {
            thread {
                try {
                    ProcessBuilder("input", "tap", currentX.toInt().toString(), currentY.toInt().toString())
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                    Log.d("CursorOverlayService", "ADB tap injected successfully at ($currentX, $currentY)")
                } catch (e: Exception) {
                    Log.e("CursorOverlayService", "Failed to inject ADB tap", e)
                }
            }
            return
        }
        GtvAccessibilityService.getSharedInstance()?.injectClick(currentX, currentY)
    }

    private fun triggerSystemKey(key: String) {
        if (isAdbMode) {
            val keycode = when (key) {
                "BACK" -> 4
                "HOME" -> 3
                "MENU" -> 82
                else -> 0
            }
            if (keycode != 0) {
                thread {
                    try {
                        Runtime.getRuntime().exec("input keyevent $keycode").waitFor()
                        Log.d("CursorOverlayService", "Injected ADB keyevent $keycode")
                    } catch (e: Exception) {
                        Log.e("CursorOverlayService", "Failed ADB keyevent", e)
                    }
                }
            }
        } else {
            val action = when (key) {
                "BACK" -> AccessibilityService.GLOBAL_ACTION_BACK
                "HOME" -> AccessibilityService.GLOBAL_ACTION_HOME
                "MENU" -> AccessibilityService.GLOBAL_ACTION_RECENTS // fallback for menu on ATV
                else -> 0
            }
            if (action != 0) {
                GtvAccessibilityService.getSharedInstance()?.performGlobalAction(action)
                Log.d("CursorOverlayService", "Dispatched Accessibility global action $action")
            }
        }
    }

    private fun setTvBrightness(value: Int) {
        try {
            val brightness = value.coerceIn(0, 255)
            if (!Settings.System.canWrite(this)) {
                Log.w("CursorOverlayService", "WRITE_SETTINGS not granted, launching request...")
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } else {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Log.d("CursorOverlayService", "Set screen brightness: $brightness")
            }
        } catch (e: Exception) {
            Log.e("CursorOverlayService", "Failed to set TV brightness", e)
        }
    }

    private fun setTvVolume(value: Int) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val scaledVolume = ((value / 15.0f) * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, scaledVolume, AudioManager.FLAG_SHOW_UI)
            Log.d("CursorOverlayService", "Set media volume: $scaledVolume / $maxVolume")
        } catch (e: Exception) {
            Log.e("CursorOverlayService", "Failed to set TV media volume", e)
        }
    }

    private fun setTvPictureMode(mode: String) {
        try {
            val modeValue = when (mode) {
                "DYNAMIC" -> "dynamic"
                "GAME" -> "game"
                "CINEMA" -> "cinema"
                else -> "standard"
            }
            if (Settings.System.canWrite(this)) {
                Settings.System.putString(contentResolver, "picture_quality_mode", modeValue)
            }
            Log.d("CursorOverlayService", "Attempted picture mode set: $modeValue")
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d("CursorOverlayService", "Launched TV display/picture overlay settings.")
        } catch (e: Exception) {
            Log.e("CursorOverlayService", "Failed to set TV picture mode", e)
        }
    }
}
