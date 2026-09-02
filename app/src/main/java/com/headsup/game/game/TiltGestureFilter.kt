package com.headsup.game.game

import kotlin.math.abs

/**
 * Pure state machine behind [TiltDetector]: feed it gravity-Z samples and it
 * fires exactly one gesture per tilt. The phone must be near vertical (the
 * neutral zone) before the first gesture and again between gestures.
 *
 * Samples are low-pass filtered. The filter is seeded from the first sample
 * rather than 0 so a phone that starts flat doesn't look "neutral" for one
 * frame and then fire as the filter catches up.
 */
class TiltGestureFilter(
    private val onTiltDown: () -> Unit,
    private val onTiltUp: () -> Unit,
    private val triggerThreshold: Float = 7f,
    private val neutralThreshold: Float = 4f,
    private val lowPassAlpha: Float = 0.35f,
) {
    private var armed = false
    private var filteredZ = 0f
    private var hasSample = false

    fun reset() {
        armed = false
        filteredZ = 0f
        hasSample = false
    }

    /** @param z gravity along the axis out of the screen, in m/s². */
    fun onSample(z: Float) {
        if (!hasSample) {
            filteredZ = z
            hasSample = true
        } else {
            filteredZ += lowPassAlpha * (z - filteredZ)
        }
        if (!armed) {
            if (abs(filteredZ) < neutralThreshold) armed = true
            return
        }
        when {
            filteredZ < -triggerThreshold -> {
                armed = false
                onTiltDown()
            }
            filteredZ > triggerThreshold -> {
                armed = false
                onTiltUp()
            }
        }
    }
}
