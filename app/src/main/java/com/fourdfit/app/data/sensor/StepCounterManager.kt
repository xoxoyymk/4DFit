package com.fourdfit.app.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.fourdfit.app.domain.model.StepStatus
import com.fourdfit.app.domain.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Reads the hardware step counter (TYPE_STEP_COUNTER, cumulative since boot) and converts it
 * into a per-day count stored in Room. Counting happens in hardware, so steps taken while the
 * app is closed are picked up the next time it opens. Handles reboots via a stored offset.
 * Values are approximate and intended for general activity awareness only.
 */
class StepCounterManager(
    context: Context,
    private val activityRepository: ActivityRepository,
    private val scope: CoroutineScope,
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = appContext.getSharedPreferences("fourdfit_steps", Context.MODE_PRIVATE)
    private var registered = false

    private val _status = MutableStateFlow(computeStatus())
    val status: StateFlow<StepStatus> = _status.asStateFlow()

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Call on resume and after a permission result. Starts listening when possible. */
    fun refresh() {
        _status.value = computeStatus()
        if (_status.value == StepStatus.ACTIVE) start()
    }

    fun start() {
        if (registered || sensor == null || !hasPermission()) return
        registered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) == true
    }

    fun stop() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    fun resetForLogout() {
        stop()
        prefs.edit().clear().apply()
    }

    private fun computeStatus(): StepStatus =
        when {
            sensor == null -> StepStatus.UNAVAILABLE
            !hasPermission() -> StepStatus.NEEDS_PERMISSION
            else -> StepStatus.ACTIVE
        }

    override fun onSensorChanged(event: SensorEvent) {
        val value = event.values.firstOrNull()?.toLong() ?: return
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(KEY_DAY, Long.MIN_VALUE)
        var baseline = prefs.getLong(KEY_BASELINE, -1L)
        var offset = prefs.getLong(KEY_OFFSET, 0L)
        val last = prefs.getLong(KEY_LAST, -1L)

        if (storedDay != today) {
            // New day: steps since the last reading of the previous day count toward today.
            baseline = if (last in 0..value) last else value
            offset = 0L
        } else if (baseline < 0) {
            baseline = value
        } else if (value < last) {
            // Device rebooted: the counter restarted from zero.
            offset += (last - baseline).coerceAtLeast(0)
            baseline = 0L
        }
        val steps = (offset + (value - baseline)).coerceAtLeast(0).toInt()
        prefs
            .edit()
            .putLong(KEY_DAY, today)
            .putLong(KEY_BASELINE, baseline)
            .putLong(KEY_OFFSET, offset)
            .putLong(KEY_LAST, value)
            .apply()
        scope.launch { activityRepository.setSteps(LocalDate.ofEpochDay(today), steps) }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_BASELINE = "baseline"
        const val KEY_OFFSET = "offset"
        const val KEY_LAST = "last"
    }
}
