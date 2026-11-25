package ru.moneyapp.util

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Last-crash journal.
 *
 * Installs a default uncaught-exception handler that stores the stack trace
 * (thread + throwable) in SharedPreferences BEFORE handing the exception to
 * the system handler, so the process still dies as usual but the next launch
 * can show/share the cause in Settings → «О приложении».
 */
object CrashLog {

    private const val PREFS = "crash_log"
    private const val KEY_LAST = "last_crash"
    private const val MAX_LINES = 40

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            save(app, thread, throwable)
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    fun save(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val text = buildString {
                appendLine("thread=${thread.name} (id ${thread.id})")
                appendLine("device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE}")
                throwable.stackTraceToString().lineSequence()
                    .take(MAX_LINES)
                    .forEach { appendLine(it) }
            }
            // commit() (not apply): the process dies right after, an async
            // write could be lost.
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST, text).commit()
        } catch (_: Exception) {
            // Never interfere with the crash itself.
        }
    }

    fun last(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LAST).apply()
    }
}
