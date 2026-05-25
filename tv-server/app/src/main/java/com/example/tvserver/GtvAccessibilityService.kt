package com.example.tvserver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.concurrent.thread

class GtvAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GtvAccessibilityService? = null
            private set

        fun getSharedInstance(): GtvAccessibilityService? {
            return instance
        }

        fun isServiceRunning(): Boolean {
            return instance != null
        }
    }

    private var lastHoverTime: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("GtvAccessibilityService", "Google TV remote accessibility service bound successfully!")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.i("GtvAccessibilityService", "Accessibility service unbound.")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun updateHoverTarget(x: Float, y: Float) {
        if (!CursorOverlayService.isCursorEnabled) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastHoverTime < 120) {
            return
        }
        lastHoverTime = now
        thread {
            try {
                val root = rootInActiveWindow
                if (root != null) {
                    val targetNode = findNodeAtRecursive(root, x.toInt(), y.toInt())
                    if (targetNode != null) {
                        var focusNode = targetNode
                        while (focusNode != null) {
                            if (focusNode.isFocusable || focusNode.isClickable) {
                                focusNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                                focusNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                                focusNode.recycle()
                                break
                            }
                            val parent = focusNode.parent
                            focusNode.recycle()
                            focusNode = parent
                        }
                    } else {
                        // Clear focus when entering empty area
                        root.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
                    }
                    root.recycle()
                }
            } catch (e: Exception) {
                Log.e("GtvAccessibilityService", "Error in updateHoverTarget", e)
            }
        }
    }

    fun injectClick(x: Float, y: Float) {
        if (!CursorOverlayService.isCursorEnabled) {
            return
        }
        try {
            Log.d("GtvAccessibilityService", "Inject click requested at ($x, $y)")
            val root = rootInActiveWindow
            var focusNode: AccessibilityNodeInfo? = null
            if (root != null) {
                val targetNode = findNodeAtRecursive(root, x.toInt(), y.toInt())
                if (targetNode != null) {
                    var tempNode: AccessibilityNodeInfo? = targetNode
                    while (tempNode != null) {
                        if (tempNode.isFocusable || tempNode.isClickable) {
                            tempNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            tempNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                            Log.d("GtvAccessibilityService", "Actively focused node under cursor: ${tempNode.className}")
                            focusNode = tempNode
                            break
                        }
                        val parent = tempNode.parent
                        tempNode.recycle()
                        tempNode = parent
                    }
                }
            }

            var clicked = false
            // 1. Direct D-pad Center injection via process call
            try {
                Runtime.getRuntime().exec(arrayOf("input", "keyevent", "23")).waitFor()
                clicked = true
                Log.i("GtvAccessibilityService", "Successfully injected DPAD_CENTER keyevent 23")
            } catch (e: Exception) {
                Log.e("GtvAccessibilityService", "Failed to inject DPAD_CENTER keyevent", e)
            }

            // 2. ProcessBuilder Coordinate Tap injection
            if (!clicked) {
                try {
                    Runtime.getRuntime().exec(arrayOf("input", "tap", x.toInt().toString(), y.toInt().toString())).waitFor()
                    clicked = true
                    Log.i("GtvAccessibilityService", "Successfully injected coordinate tap via ProcessBuilder")
                } catch (e: Exception) {
                    Log.e("GtvAccessibilityService", "Failed to inject coordinate tap", e)
                }
            }

            // 3. Accessibility Tree Click Fallback
            if (!clicked && focusNode != null) {
                var clickableNode: AccessibilityNodeInfo? = focusNode
                while (clickableNode != null) {
                    val className = clickableNode.className?.toString() ?: ""
                    val isWebView = className.contains("WebView", ignoreCase = true) || className.contains("Cobalt", ignoreCase = true)
                    
                    if (!isWebView && clickableNode.isClickable) {
                        if (clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i("GtvAccessibilityService", "Accessibility ACTION_CLICK fallback success on class: $className")
                            clicked = true
                            clickableNode.recycle()
                            break
                        }
                    }
                    val parent = clickableNode.parent
                    clickableNode.recycle()
                    clickableNode = parent
                }
            }

            // 4. Gesture Simulation Fallback
            if (!clicked) {
                val builder = GestureDescription.Builder()
                val path = Path()
                path.moveTo(x, y)
                path.lineTo(x + 1.0f, y + 1.0f)
                builder.addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
                dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d("GtvAccessibilityService", "Gesture fallback completed at ($x, $y)")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w("GtvAccessibilityService", "Gesture fallback cancelled at ($x, $y)")
                    }
                }, null)
            }

            focusNode?.recycle()
            root?.recycle()
        } catch (e: Exception) {
            Log.e("GtvAccessibilityService", "Failed to inject click", e)
        }
    }

    private fun findNodeAtRecursive(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) {
            return null
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeAtRecursive(child, x, y)
                if (result != null) {
                    return result
                }
                child.recycle()
            }
        }
        return node
    }
}
