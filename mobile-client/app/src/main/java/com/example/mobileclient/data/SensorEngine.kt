package com.example.mobileclient.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SensorEngine(
    context: Context,
    private val onMotionDetected: (dx: Float, dy: Float) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val deadzoneThreshold = 0.03f
    var sensitivityX = 40.0f
    var sensitivityY = 40.0f
    private val calibrationWindowMs = 1500L

    var isAimingActive = false
    private var lastMovementTime = System.currentTimeMillis()
    private val gyroSamples = ArrayList<FloatArray>()
    private val accelSamples = ArrayList<FloatArray>()

    private var biasX = 0.0f
    private var biasY = 0.0f
    private var biasZ = 0.0f

    fun start() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        lastMovementTime = System.currentTimeMillis()
        gyroSamples.clear()
        accelSamples.clear()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values.clone())
            }
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscope(event.values.clone())
            }
        }
    }

    private fun processAccelerometer(values: FloatArray) {
        synchronized(accelSamples) {
            accelSamples.add(values)
            if (accelSamples.size > 100) {
                accelSamples.removeAt(0)
            }
        }
    }

    private fun processGyroscope(values: FloatArray) {
        val rawX = values[0]
        val rawY = values[1]
        val rawZ = values[2]

        checkAutoCalibration(rawX, rawY, rawZ)

        val calX = rawX - biasX
        val calZ = rawZ - biasZ

        if (isAimingActive) {
            val finalX = if (Math.abs(calX) < deadzoneThreshold) 0.0f else calX
            val finalZ = if (Math.abs(calZ) < deadzoneThreshold) 0.0f else calZ

            if (finalX == 0.0f && finalZ == 0.0f) {
                return
            }

            // Map yaw (-finalZ) and pitch (-finalX) to screen relative dx, dy deltas
            val dx = (-finalZ) * sensitivityX
            val dy = (-finalX) * sensitivityY
            onMotionDetected(dx, dy)
        }
    }

    private fun checkAutoCalibration(gx: Float, gy: Float, gz: Float) {
        val currentTime = System.currentTimeMillis()
        val gyroMagnitude = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()

        if (gyroMagnitude > 0.08f) {
            lastMovementTime = currentTime
            synchronized(gyroSamples) {
                gyroSamples.clear()
            }
            return
        }

        synchronized(gyroSamples) {
            gyroSamples.add(floatArrayOf(gx, gy, gz))
            if (gyroSamples.size > 100) {
                gyroSamples.removeAt(0)
            }
        }

        if (currentTime - lastMovementTime >= calibrationWindowMs) {
            synchronized(gyroSamples) {
                if (gyroSamples.isNotEmpty()) {
                    var sumX = 0.0f
                    var sumY = 0.0f
                    var sumZ = 0.0f
                    for (sample in gyroSamples) {
                        sumX += sample[0]
                        sumY += sample[1]
                        sumZ += sample[2]
                    }
                    biasX = sumX / gyroSamples.size
                    biasY = sumY / gyroSamples.size
                    biasZ = sumZ / gyroSamples.size
                    lastMovementTime = currentTime
                    gyroSamples.clear()
                }
            }
        }
    }

    fun resetCalibration() {
        biasX = 0.0f
        biasY = 0.0f
        biasZ = 0.0f
        gyroSamples.clear()
        accelSamples.clear()
        lastMovementTime = System.currentTimeMillis()
    }
}
