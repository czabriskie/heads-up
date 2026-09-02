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
 * Thin sensor wrapper; the gesture logic lives in [TiltGestureFilter].
 */
class TiltDetector(
    context: Context,
    onTiltDown: () -> Unit,
    onTiltUp: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val filter = TiltGestureFilter(onTiltDown, onTiltUp)

    fun start() {
        filter.reset()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        filter.onSample(event.values[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
