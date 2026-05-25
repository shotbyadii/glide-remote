package com.example.mobileclient.data

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {
    private var lastHoverHapticTime = 0L
    private const val HOVER_THROTTLE_MS = 50L

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun performTapHaptic(context: Context) {
        if (!ProfileManager.isHapticTapEnabled(context)) return

        val vibrator = getVibrator(context) ?: return
        val intensity = ProfileManager.getHapticTapIntensity(context).coerceIn(0.0f, 1.0f)
        val amplitude = (intensity * 255).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (vibrator.hasAmplitudeControl() && intensity != 0.8f) {
                    VibrationEffect.createOneShot(30L, amplitude)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
            } else {
                VibrationEffect.createOneShot(30L, amplitude)
            }

            try {
                vibrator.vibrate(effect, audioAttributes)
            } catch (e: Exception) {
                try {
                    vibrator.vibrate(effect)
                } catch (ex: Exception) {
                    vibrator.vibrate(30L)
                }
            }
        } else {
            vibrator.vibrate(30L)
        }
    }

    fun performHoverHaptic(context: Context) {
        if (!ProfileManager.isHapticHoverEnabled(context)) return

        val now = System.currentTimeMillis()
        if (now - lastHoverHapticTime < HOVER_THROTTLE_MS) {
            return
        }
        lastHoverHapticTime = now

        val vibrator = getVibrator(context) ?: return
        val intensity = ProfileManager.getHapticHoverIntensity(context).coerceIn(0.0f, 1.0f)
        val amplitude = (intensity * 255).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (vibrator.hasAmplitudeControl() && intensity != 0.4f) {
                    VibrationEffect.createOneShot(12L, amplitude)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
            } else {
                VibrationEffect.createOneShot(12L, amplitude)
            }

            try {
                vibrator.vibrate(effect, audioAttributes)
            } catch (e: Exception) {
                try {
                    vibrator.vibrate(effect)
                } catch (ex: Exception) {
                    vibrator.vibrate(10L)
                }
            }
        } else {
            vibrator.vibrate(10L)
        }
    }
}

