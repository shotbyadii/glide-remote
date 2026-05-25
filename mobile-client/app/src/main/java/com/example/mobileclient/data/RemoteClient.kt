package com.example.mobileclient.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class RemoteClient(private val context: Context) {
    private val tcpPort = 8080
    private val udpPort = 8081
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    var isConnected = false
    var tvIpAddress: String? = null
    var tvDeviceName: String? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var resolvedServiceInfo: NsdServiceInfo? = null
    private var webSocketClient: WebSocketClient? = null
    private var udpSocket: DatagramSocket? = null
    private var tvInetAddress: InetAddress? = null
    private var sequenceNumber = 0

    fun startDiscovery() {
        Log.i("RemoteClient", "Starting NSD Service Discovery...")
        nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("RemoteClient", "NSD Start Discovery Failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("RemoteClient", "NSD Stop Discovery Failed: $errorCode")
                stopDiscovery()
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.i("RemoteClient", "NSD Discovery Started: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("RemoteClient", "NSD Discovery Stopped: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.i("RemoteClient", "NSD Service Found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == "_gtvremote._tcp.") {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("RemoteClient", "NSD Resolve Failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.i("RemoteClient", "NSD Service Resolved: ${serviceInfo.serviceName} at ${serviceInfo.host.hostAddress}")
                            resolvedServiceInfo = serviceInfo
                            tvDeviceName = serviceInfo.serviceName
                            connectToTv(serviceInfo.host.hostAddress)
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.w("RemoteClient", "NSD Service Lost: ${serviceInfo.serviceName}")
                if (resolvedServiceInfo?.serviceName == serviceInfo.serviceName) {
                    disconnect()
                }
            }
        }
        nsdManager.discoverServices("_gtvremote._tcp", NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
    }

    fun stopDiscovery() {
        try {
            nsdDiscoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
            }
        } catch (e: Exception) {
            Log.e("RemoteClient", "Failed to stop NSD discovery", e)
        }
        nsdDiscoveryListener = null
    }

    fun connectToTv(ipAddress: String) {
        if (isConnected) return
        thread {
            try {
                tvInetAddress = InetAddress.getByName(ipAddress)
                tvIpAddress = ipAddress
                if (tvDeviceName == null) {
                    tvDeviceName = "Manual Device"
                }
                val wsUri = URI("ws://$ipAddress:$tcpPort")
                webSocketClient = object : WebSocketClient(wsUri) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        Log.i("RemoteClient", "WebSocket channel connected successfully!")
                        isConnected = true
                        onConnectionStateChanged?.invoke(true)
                    }

                    override fun onMessage(message: String?) {}

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        Log.i("RemoteClient", "WebSocket channel closed: $reason")
                        isConnected = false
                        onConnectionStateChanged?.invoke(false)
                    }

                    override fun onError(ex: Exception?) {
                        Log.e("RemoteClient", "WebSocket error occurred", ex)
                        isConnected = false
                        onConnectionStateChanged?.invoke(false)
                    }
                }
                webSocketClient?.connect()
                udpSocket = DatagramSocket()
            } catch (e: Exception) {
                Log.e("RemoteClient", "Failed to establish remote pipeline connections", e)
            }
        }
    }

    fun disconnect() {
        stopDiscovery()
        try {
            webSocketClient?.close()
        } catch (e: Exception) {
            Log.e("RemoteClient", "Failed to close websocket", e)
        }
        try {
            udpSocket?.close()
        } catch (e: Exception) {
            Log.e("RemoteClient", "Failed to close UDP socket", e)
        }
        isConnected = false
        tvIpAddress = null
        tvDeviceName = null
        onConnectionStateChanged?.invoke(false)
        Log.i("RemoteClient", "Disconnected from GTV Server")
    }

    fun sendMotion(dx: Float, dy: Float, type: Byte) {
        val socket = udpSocket ?: return
        val inetAddress = tvInetAddress ?: return
        thread {
            try {
                sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                val buffer = ByteArray(12)
                buffer[0] = type
                val byteBuffer = ByteBuffer.wrap(buffer)
                byteBuffer.putFloat(1, dx)
                byteBuffer.putFloat(5, dy)
                byteBuffer.putShort(9, sequenceNumber.toShort())
                buffer[11] = 0.toByte()

                val packet = DatagramPacket(buffer, buffer.size, inetAddress, udpPort)
                socket.send(packet)
            } catch (e: Exception) {
                Log.e("RemoteClient", "UDP Packet Send Failed", e)
            }
        }
    }

    private fun sendJson(json: JSONObject) {
        thread {
            try {
                webSocketClient?.let {
                    if (it.isOpen) {
                        it.send(json.toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteClient", "Failed to send websocket json", e)
            }
        }
    }

    fun sendClick(isDown: Boolean) {
        val json = JSONObject()
        json.put("type", "INPUT_EVENT")
        json.put("action", "CLICK")
        json.put("state", if (isDown) "DOWN" else "UP")
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendSystemKey(key: String) {
        val json = JSONObject()
        json.put("type", "SYSTEM_KEY")
        json.put("key", key)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendBrightness(value: Int) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "BRIGHTNESS")
        json.put("value", value)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendVolume(value: Int) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "VOLUME")
        json.put("value", value)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendPictureMode(mode: String) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "PICTURE_MODE")
        json.put("value", mode)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendLaunchSetting(value: String) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "LAUNCH_SETTING")
        json.put("value", value)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendAcceleration(value: Float) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "ACCELERATION")
        json.put("value", value)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }

    fun sendFriction(value: Float) {
        val json = JSONObject()
        json.put("type", "SYSTEM_CONTROL")
        json.put("action", "FRICTION")
        json.put("value", value)
        json.put("timestamp", System.currentTimeMillis())
        sendJson(json)
    }
}
