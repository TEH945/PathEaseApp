package com.example.patheaseapp.Hazard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

// Listens to the phone's accelerometer and calls onBumpDetected() whenever a
// sudden jolt is felt (rough/bumpy road), with a cooldown so one bump isn't
// reported multiple times in a row. No runtime permission needed for this sensor.
@Composable
fun DetectRoadBumps(
    thresholdMetersPerSecondSquared: Float = 20f, // tune this after testing on a real device
    cooldownMillis: Long = 5000L,
    onBumpDetected: () -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastBumpTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val magnitude = sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]
                )
                val now = System.currentTimeMillis()
                if (magnitude > thresholdMetersPerSecondSquared && (now - lastBumpTime) > cooldownMillis) {
                    lastBumpTime = now
                    onBumpDetected()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }
}