package com.example.tvserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject

class RemoteService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): RemoteService = this@RemoteService
    }

    private val binder = LocalBinder()
    private var isRunning = false
    private var nsdManager: NsdManager? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
    private var webSocketServer: GtvWebSocketServer? = null
    private var udpSocket: DatagramSocket? = null

    private val tcpPort = 8080
    private val udpPort = 8081
    private val channelId = "gtv_remote_channel"
    private val notificationId = 1001

    var targetX = 960.0f
    var targetY = 540.0f
    private var lastSequenceNumber = -1

    var onPositionUpdated: ((Float, Float) -> Unit)? = null
    var onClickReceived: ((Boolean) -> Unit)? = null
    var onKeyReceived: ((String) -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startRemoteServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopRemoteServer()
        super.onDestroy()
    }

    private fun startRemoteServer() {
        if (isRunning) return
        isRunning = true
        Log.i("RemoteService", "Starting GTV Remote foreground servers...")
        promoteToForeground()

        webSocketServer = GtvWebSocketServer(tcpPort)
        webSocketServer?.start()

        startUdpReceiver()
        registerNsdService()
    }

    private fun promoteToForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GTV Remote Connection Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps GTV Remote sockets listening when app is in the background."
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GTV Remote Active")
            .setContentText("Listening for mobile connection events on Wi-Fi...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun stopRemoteServer() {
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        try {
            nsdManager?.unregisterService(nsdRegistrationListener)
        } catch (e: Exception) {
            Log.e("RemoteService", "Failed to unregister NSD", e)
        }
        try {
            webSocketServer?.stop()
        } catch (e: Exception) {
            Log.e("RemoteService", "Failed to stop WebSocket server", e)
        }
        udpSocket?.close()
        Log.i("RemoteService", "GTV Remote servers stopped successfully.")
    }

    private fun registerNsdService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "GTV-Hybrid-Remote"
            serviceType = "_gtvremote._tcp"
            port = tcpPort
        }
        val manager = getSystemService(NSD_SERVICE) as NsdManager
        nsdRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i("RemoteService", "NSD successfully registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("RemoteService", "NSD registration failed: Error $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i("RemoteService", "NSD unregistered successfully")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("RemoteService", "NSD unregistration failed: Error $errorCode")
            }
        }
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
        nsdManager = manager
    }

    private fun startUdpReceiver() {
        thread(name = "UDP-Receiver-Thread") {
            try {
                udpSocket = DatagramSocket(udpPort)
                val buffer = ByteArray(12)
                val packet = DatagramPacket(buffer, buffer.size)
                Log.i("RemoteService", "UDP server started on port $udpPort")
                while (isRunning) {
                    try {
                        udpSocket?.receive(packet)
                        val data = packet.data
                        val byteBuffer = ByteBuffer.wrap(data)
                        // structure: type (byte), dx (float), dy (float), sequence (short)
                        val dx = byteBuffer.getFloat(1)
                        val dy = byteBuffer.getFloat(5)
                        val sequence = byteBuffer.getShort(9).toInt() and 0xFFFF

                        if (sequence > lastSequenceNumber || lastSequenceNumber - sequence > 30000) {
                            lastSequenceNumber = sequence
                            targetX += dx
                            targetY += dy

                            onPositionUpdated?.invoke(dx, dy)
                            RemoteServiceCallbacks.onPositionUpdated?.invoke(dx, dy)
                        }
                    } catch (e: IOException) {
                        if (!isRunning) break
                        Log.e("RemoteService", "UDP read error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteService", "Failed to bind UDP socket", e)
            }
        }
    }

    private inner class GtvWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            Log.i("RemoteService", "WebSocket client connected: ${conn?.remoteSocketAddress}")
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.i("RemoteService", "WebSocket connection closed: ${conn?.remoteSocketAddress}")
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            if (message == null) return
            try {
                val json = JSONObject(message)
                val type = json.optString("type") ?: json.optString("@type")
                if (type != null) {
                    when (type) {
                        "INPUT_EVENT" -> {
                            val action = json.optString("action")
                            val state = json.optString("state")
                            if (action == "CLICK") {
                                val isDown = (state == "DOWN")
                                onClickReceived?.invoke(isDown)
                                RemoteServiceCallbacks.onClickReceived?.invoke(isDown)
                            }
                        }
                        "SYSTEM_CONTROL" -> {
                            val action = json.optString("action")
                            if (action != null) {
                                when (action) {
                                    "VOLUME" -> {
                                        val value = json.optInt("value")
                                        RemoteServiceCallbacks.onVolumeChanged?.invoke(value)
                                    }
                                    "FRICTION" -> {
                                        val value = json.optDouble("value").toFloat()
                                        CursorOverlayService.momentumFriction = value
                                    }
                                    "BRIGHTNESS" -> {
                                        val value = json.optInt("value")
                                        RemoteServiceCallbacks.onBrightnessChanged?.invoke(value)
                                    }
                                    "LAUNCH_SETTING" -> {
                                        val value = json.optString("value")
                                        RemoteServiceCallbacks.onLaunchSetting?.invoke(value)
                                    }
                                    "PICTURE_MODE" -> {
                                        val value = json.optString("value")
                                        RemoteServiceCallbacks.onPictureModeChanged?.invoke(value)
                                    }
                                    "ACCELERATION" -> {
                                        val value = json.optDouble("value").toFloat()
                                        CursorOverlayService.acceleration = value
                                    }
                                }
                            }
                        }
                        "SYSTEM_KEY" -> {
                            val key = json.optString("key")
                            if (key != null) {
                                onKeyReceived?.invoke(key)
                                RemoteServiceCallbacks.onKeyReceived?.invoke(key)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteService", "Failed to parse WebSocket JSON: $message", e)
            }
        }

        override fun onError(conn: WebSocket?, ex: java.lang.Exception?) {
            Log.e("RemoteService", "WebSocket error", ex)
        }

        override fun onStart() {
            Log.i("RemoteService", "WebSocket server started on TCP port $tcpPort")
        }
    }
}
