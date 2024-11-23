package org.nitri.opentopo.util

import android.util.Log
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


class MapOrientation {

    private lateinit var animationJob: Job
    private var mMapOrientation = 0f
    private var mTargetMapOrientation = 0f
    private var mPreviousMapOrientation = 0f

    private val orientationAnimationInterpolator: Interpolator = AccelerateDecelerateInterpolator()

    private var mMapOrientationAnimationRunning = false

    /**
     * Target orientation based on heading. Start orientation animation on change above noise threshold.
     *
     * @param degrees 0 - 360 degrees
     */
    fun setTargetMapOrientation(mapView: MapView, degrees: Float) {
        mTargetMapOrientation = degrees

        val roundedTargetMapOrientation = mTargetMapOrientation.roundToInt()
        val roundedPreviousMapOrientation = mPreviousMapOrientation.roundToInt()

        if (abs(mTargetMapOrientation - mPreviousMapOrientation) > ORIENTATION_EPSILON
            && roundedTargetMapOrientation != roundedPreviousMapOrientation
        ) {
            if (!mMapOrientationAnimationRunning) {
                animateToMapOrientation(mapView, mMapOrientation, 360 - mTargetMapOrientation)
            }
        }
    }

    /**
     * Smoothly animate to the target orientation.
     *
     * @param originalOrientation  deg
     * @param targetMapOrientation deg
     */
    private fun animateToMapOrientation(mapView: MapView, originalOrientation: Float, targetMapOrientation: Float) {

        animationJob.cancel()

        val angularDistance = angularDistance(targetMapOrientation, originalOrientation)
        val numberOfSteps = (abs(angularDistance) / ORIENTATION_ANIMATION_STEP_SIZE).toInt()

        animationJob = CoroutineScope(Dispatchers.Main).launch {
            mMapOrientationAnimationRunning = true

            for (step in 1..numberOfSteps) {
                val timeIndex = step.toFloat() / numberOfSteps
                val angularProgress = orientationAnimationInterpolator.getInterpolation(timeIndex)
                mMapOrientation = originalOrientation + angularDistance * angularProgress

                mMapOrientation = (mMapOrientation + 360) % 360  // Keep within 0-360 range

                mPreviousMapOrientation = mMapOrientation
                mapView.mapOrientation = mMapOrientation

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

    companion object {
        private val TAG = MapOrientation::class.java.simpleName
        const val ORIENTATION_ANIMATION_STEP_SIZE: Float = 0.1f // degrees
        const val ORIENTATION_ANIMATION_DELTA_TIME: Int = 3 // ms
        const val ORIENTATION_EPSILON: Int = 10 // noise threshold value
    }
}