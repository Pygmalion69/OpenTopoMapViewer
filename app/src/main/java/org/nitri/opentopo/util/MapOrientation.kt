package org.nitri.opentopo.util

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import kotlin.math.abs
import kotlin.math.roundToInt


object MapOrientation {

    private val TAG = MapOrientation::class.java.simpleName
    private const val ORIENTATION_ANIMATION_STEP_SIZE: Float = 0.1f // degrees
    private const val ORIENTATION_ANIMATION_DELTA_TIME: Int = 3 // ms
    private const val ORIENTATION_EPSILON: Int = 10 // noise threshold value

    private lateinit var animationJob: Job
    private var mapOrientation = 0f
    private var targetMapOrientation = 0f
    private var previousMapOrientation = 0f

    private val orientationAnimationInterpolator: Interpolator = AccelerateDecelerateInterpolator()

    private var mMapOrientationAnimationRunning = false

    val currentMapOrientation: Float
        get() = mapOrientation

    /**
     * Target orientation based on heading. Start orientation animation on change above noise threshold.
     *
     * @param degrees 0 - 360 degrees
     */
    fun setTargetMapOrientation(mapView: MapView, degrees: Float) {
        targetMapOrientation = degrees

        val roundedTargetMapOrientation = targetMapOrientation.roundToInt()
        val roundedPreviousMapOrientation = previousMapOrientation.roundToInt()

        if (abs(targetMapOrientation - previousMapOrientation) > ORIENTATION_EPSILON
            && roundedTargetMapOrientation != roundedPreviousMapOrientation
        ) {
            if (!mMapOrientationAnimationRunning) {
                animateToMapOrientation(mapView, mapOrientation, 360 - targetMapOrientation)
            }
        }
    }

    /**
     * Smoothly animate to the target orientation.
     *
     * @param originalOrientation  deg
     * @param targetMapOrientation deg
     */
    private fun animateToMapOrientation(
        mapView: MapView,
        originalOrientation: Float,
        targetMapOrientation: Float
    ) {

        animationJob.cancel()

        val angularDistance = angularDistance(targetMapOrientation, originalOrientation)
        val numberOfSteps = (abs(angularDistance) / ORIENTATION_ANIMATION_STEP_SIZE).toInt()

        animationJob = CoroutineScope(Dispatchers.Main).launch {
            mMapOrientationAnimationRunning = true

            for (step in 1..numberOfSteps) {
                val timeIndex = step.toFloat() / numberOfSteps
                val angularProgress = orientationAnimationInterpolator.getInterpolation(timeIndex)
                mapOrientation = originalOrientation + angularDistance * angularProgress

                mapOrientation = (mapOrientation + 360) % 360  // Keep within 0-360 range

                previousMapOrientation = mapOrientation
                mapView.mapOrientation = mapOrientation

                delay(ORIENTATION_ANIMATION_DELTA_TIME.toLong())
            }

            mMapOrientationAnimationRunning = false
        }
    }

    private fun angularDistance(alpha: Float, beta: Float): Float {
        val phi = abs(beta - alpha) % 360
        val distance = if (phi > 180) 360 - phi else phi
        return distance * if ((alpha - beta + 360) % 360 > 180) -1 else 1
    }
}