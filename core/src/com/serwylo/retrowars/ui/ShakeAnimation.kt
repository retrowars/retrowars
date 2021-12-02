package com.serwylo.retrowars.ui

import kotlin.math.sin

class ShakeAnimation(
    private val duration: Float,
    private val frequency: Int,
    private val distance: Float,
) {

    private var yPosition = 0f
    private var totalDuration = 0f
    private var currentDuration = 0f
    private var shakeAmplitude = 0f

    fun shake() {
        totalDuration = if (totalDuration <= 0f) duration else totalDuration + duration
        shakeAmplitude = distance
    }

    /**
     * Most the time you can just use the return value from the [update] method, which is the
     * *relative amount of change* since the last update. However sometimes you may wish to
     * know the absolute position of this shake.
     */
    fun getY() = yPosition

    fun update(delta: Float): Float {
        if (totalDuration <= 0) {
            return 0f
        }

        val toShift: Float

        currentDuration += delta

        if (currentDuration >= totalDuration) {

            toShift = -yPosition
            totalDuration = 0f
            currentDuration = 0f
            yPosition = 0f

        } else {

            //  +------------------|----+
            //  0       1       2       3
            var factor = currentDuration / totalDuration * frequency
            while (factor > 1) {
                factor -= 1
            }
            val radians = factor * Math.PI * 2
            val desiredPosition = (sin(radians) * shakeAmplitude - shakeAmplitude / 2).toFloat()
            val shift = desiredPosition - yPosition

            toShift = shift
            yPosition += shift

        }

        return toShift

    }
}