package com.headsup.game.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects the Heads Up gestures while the phone is held against the forehead
 * in landscape, screen facing the other players:
 *
 *  - tilt the phone face-DOWN (screen toward the floor)  -> [onTiltDown] (correct)
 *  - tilt the phone face-UP   (screen toward the ceiling) -> [onTiltUp]  (pass)
 *
 * Uses the gravity component along the Z axis (out of the screen). The phone
 * must return to roughly vertical (the neutral zone) before another gesture
 * can fire, so one tilt scores exactly once.
 */
class TiltDetector(
    context: Context,
    private val onTiltDown: () -> Unit,
    private val onTiltUp: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var armed = false
    private var filteredZ = 0f

    private companion object {
        const val TRIGGER_THRESHOLD = 7f
        const val NEUTRAL_THRESHOLD = 4f
        const val LOW_PASS_ALPHA = 0.35f
    }

    fun start() {
        armed = false
        filteredZ = 0f
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        filteredZ += LOW_PASS_ALPHA * (event.values[2] - filteredZ)
        if (!armed) {
            // Wait for the phone to be held roughly vertical before accepting a gesture.
            if (kotlin.math.abs(filteredZ) < NEUTRAL_THRESHOLD) armed = true
            return
        }
        when {
            filteredZ < -TRIGGER_THRESHOLD -> {
                armed = false
                onTiltDown()
            }
            filteredZ > TRIGGER_THRESHOLD -> {
                armed = false
                onTiltUp()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
