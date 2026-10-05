package com.fourdfit.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fourdfit.app.FourDFitApp
import com.fourdfit.app.MainActivity
import com.fourdfit.app.R
import com.fourdfit.app.domain.model.ReminderType
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_REMINDERS = "fourdfit_reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel =
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notification_channel_reminders_desc) }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permitted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // checked by canNotify()
    fun show(
        context: Context,
        type: ReminderType,
        title: String,
        text: String,
    ) {
        if (!canNotify(context)) return
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pending =
            PendingIntent.getActivity(
                context,
                type.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_ID + type.ordinal, notification)
    }

    private const val NOTIFICATION_BASE_ID = 4100
}

/** Runs on the WorkManager schedule for one [ReminderType] and posts a friendly reminder. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val type = ReminderType.entries.firstOrNull { it.name == inputData.getString(KEY_TYPE) } ?: return Result.success()
        val container = (applicationContext as FourDFitApp).container
        val settings = container.settingsRepository.settings.first()
        if (settings.reminders[type] != true) return Result.success()
        if (container.tokenStore.currentToken == null) return Result.success()

        val hour = LocalTime.now().hour
        when (type) {
            ReminderType.HYDRATION -> if (hour < 9 || hour > 21) return Result.success()
            ReminderType.STREAK -> {
                val workedOutToday = container.database.workoutHistoryDao().countOnDay(LocalDate.now().toEpochDay()) > 0
                if (workedOutToday) return Result.success()
            }
            else -> Unit
        }
        val (title, text) = message(type)
        NotificationHelper.show(applicationContext, type, title, text)
        return Result.success()
    }

    private fun message(type: ReminderType): Pair<String, String> {
        val day = LocalDate.now().dayOfYear
        return when (type) {
            ReminderType.MORNING_WELLNESS ->
                "Good morning" to
                    listOf(
                        "A few deep breaths and a glass of water are a great start.",
                        "Take a moment to stretch before the day gets busy.",
                        "Today's wellness goal is waiting on your home screen.",
                    )[day % 3]
            ReminderType.WORKOUT ->
                "Time to move" to
                    listOf(
                        "Your workout for today is ready whenever you are.",
                        "Even ten minutes of movement counts.",
                        "A short session now keeps your momentum going.",
                    )[day % 3]
            ReminderType.HYDRATION -> "Hydration check" to "Have a glass of water and log it in 4D FIT."
            ReminderType.HOROSCOPE -> "Your daily horoscope" to "Today's light-hearted reading is ready. ✨"
            ReminderType.MEAL -> "Lunch time" to "Today's balanced meal idea is in your nutrition plan."
            ReminderType.STREAK -> "Keep your streak going" to "A quick mobility flow still counts for today."
        }
    }

    companion object {
        const val KEY_TYPE = "reminder_type"
    }
}

class ReminderScheduler(
    private val context: Context,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun sync(reminders: Map<ReminderType, Boolean>) {
        ReminderType.entries.forEach { type ->
            if (reminders[type] == true) schedule(type) else cancel(type)
        }
    }

    fun cancelAll() {
        ReminderType.entries.forEach { cancel(it) }
    }

    private fun schedule(type: ReminderType) {
        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(type.repeatHours, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillis(type), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type.name))
                .addTag(TAG)
                .build()
        workManager.enqueueUniquePeriodicWork(workName(type), ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun cancel(type: ReminderType) {
        workManager.cancelUniqueWork(workName(type))
    }

    private fun initialDelayMillis(type: ReminderType): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(type.hour, type.minute)
        if (type == ReminderType.HYDRATION) {
            // Next even hour inside the 9:00–21:00 window.
            while (target.isBefore(now)) target = target.plusHours(type.repeatHours)
        } else if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0)
    }

    private fun workName(type: ReminderType) = "reminder_${type.name.lowercase()}"

    private companion object {
        const val TAG = "fourdfit_reminder"
    }
}
