package ru.moneyapp.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking
import ru.moneyapp.R
import ru.moneyapp.ui.MainActivity
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.l10n.resolveLanguage

/**
 * BroadcastReceiver for daily reminder notifications.
 * Uses AlarmManager (no FCM dependency — works on HarmonyOS / AOSP).
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "moneyapp.DAILY_REMINDER" -> {
                showReminderNotification(context)
                // On Android 12+ without SCHEDULE_EXACT_ALARM the alarm is a
                // one-shot (setAndAllowWhileIdle), so re-arm it for the next
                // day using the time carried in the alarm intent.
                if (!canUseExactAlarms(context)) {
                    scheduleDailyReminder(
                        context,
                        intent.getIntExtra(EXTRA_HOUR, 21),
                        intent.getIntExtra(EXTRA_MINUTE, 0)
                    )
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> rescheduleReminder(context)
        }
    }

    private fun canUseExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun showReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // v1.9.0: the receiver may run in a cold process where L10n.lang is
        // still at its RU default — resolve the in-app language from DataStore
        // first (bounded wait, falls back to the in-memory value on error).
        // runBlocking is deliberate: the alarm receiver must post the
        // notification before onReceive returns.
        runCatching {
            L10n.lang = runBlocking { resolveLanguage(context) }
        }
        val s = L10n.s

        // Create channel if needed (Android O+). The channel name is set
        // once, on first creation; later calls are no-ops.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                s.notifChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = s.notifChannelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // v1.9.0: localized via the in-app L10n bundle (not res strings)
            .setContentTitle(s.notifReminderTitle)
            .setContentText(s.notifReminderText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "moneyapp_reminder"
        private const val REMINDER_NOTIFICATION_ID = 1001

        /**
         * Schedule daily reminder using AlarmManager.
         * No GMS dependency — works on Huawei Mate X3 (HarmonyOS / AOSP).
         */
        fun scheduleDailyReminder(
            context: Context,
            hour: Int,
            minute: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = "moneyapp.DAILY_REMINDER"
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val trigger = getNextTriggerTime(hour, minute)
            // On Android 12+ an exact repeating alarm requires
            // SCHEDULE_EXACT_ALARM; without it setRepeating throws
            // SecurityException. Fall back to an inexact one-shot, re-armed
            // from onReceive (see EXTRA_HOUR/EXTRA_MINUTE).
            val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()
            if (exactOk) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    trigger,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trigger,
                    pendingIntent
                )
            }
        }

        fun cancelDailyReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = "moneyapp.DAILY_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun getNextTriggerTime(hour: Int, minute: Int): Long {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            calendar.set(java.util.Calendar.MINUTE, minute)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= java.util.Calendar.getInstance().timeInMillis) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis
        }

        fun rescheduleReminder(context: Context) {
            // Default reminder at 21:00
            scheduleDailyReminder(context, 21, 0)
        }

        const val EXTRA_HOUR = "extra_reminder_hour"
        const val EXTRA_MINUTE = "extra_reminder_minute"
        private const val REMINDER_REQUEST_CODE = 2001
    }
}
