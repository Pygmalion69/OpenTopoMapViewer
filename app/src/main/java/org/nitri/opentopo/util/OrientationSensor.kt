package org.nitri.opentopo.util

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import org.osmdroid.views.MapView
import kotlin.math.abs


class OrientationSensor(private val context: Context, private val mapView: MapView) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private val orientationSample: ArrayList<Float> = ArrayList()
    @Volatile private var stopped = false

    init {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        stopped = true
        sensorManager.unregisterListener(this)
    }

    companion object {
        val TAG: String = OrientationSensor::class.java.simpleName
        const val ORIENTATION_SAMPLE_SIZE: Int = 12
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //Log.d(TAG, "onSensorChanged, stopped: $stopped")
        if (stopped) return
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values

        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)

            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {

                // orientation contains azimuth, pitch and roll
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)

                val azimuth = normalizeAzimuth(orientation[0])

                // Log.d(TAG, "orientation: azimuth = $azimuth")
                orientationSample.add(azimuth)

                if (orientationSample.size >= ORIENTATION_SAMPLE_SIZE) {
                    val sanitizedSample = removeOutliers(orientationSample)
                    val averageAngle = average(sanitizedSample)
                    // Log.d(TAG, "orientation: averageAngle = $averageAngle")
                    MapOrientation.setTargetMapOrientation(mapView, averageAngle)
                    orientationSample.clear()
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    /**
     * Ensure that azimuth values are consistently between 0 and 360 degrees to avoid boundary issues
     */
    private fun normalizeAzimuth(azimuth: Float): Float {
        val degrees = Math.toDegrees(azimuth.toDouble()).toFloat()
        return (degrees + 360) % 360
    }

    /**
     * Device azimuth to target map orientation
     *
     * @param azimuth (rad)
     * @return target orientation (deg)
     */
    private fun orientation(azimuth: Float): Float {
        return if (azimuth < 0) {
            (abs((180 * azimuth).toDouble()) / Math.PI).toFloat()
        } else {
            (360 - (180 * azimuth / Math.PI)).toFloat()
        }
    }

    /**
     * Remove outliers with the interquartile ranges method (IQR)
     * http://www.mathwords.com/o/outlier.htm
     *
     * @param input dataset to sanitize
     * @return sanitized dataset
     */
    private fun removeOutliers(input: List<Float>): List<Float> {
        if (input.isEmpty()) return emptyList()

        val sortedInput = input.sorted()
        val mid = sortedInput.size / 2

        // Split into two halves
        val data1 = sortedInput.subList(0, mid)
        val data2 = if (sortedInput.size % 2 == 0) {
            sortedInput.subList(mid, sortedInput.size)
        } else {
            sortedInput.subList(mid + 1, sortedInput.size)
        }

        // Calculate Q1 and Q3
        val q1 = median(data1)
        val q3 = median(data2)

        // Calculate interquartile range (IQR)
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr

        // Filter out outliers
        return sortedInput.filter { it in lowerFence..upperFence }
    }

    private fun average(data: List<Float>): Float {
        var sum = 0f
        for (value in data) {
            sum += value
        }
        return sum / data.size
    }

    private fun median(data: List<Float>): Float {
        if (data.isEmpty()) return 0f
        val sortedData = data.sorted()
        val mid = sortedData.size / 2
        return if (sortedData.size % 2 == 0) {
            (sortedData[mid - 1] + sortedData[mid]) / 2
        } else {
            sortedData[mid]
        }
    }

}

