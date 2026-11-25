package ru.moneyapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.ui.l10n.AppLang
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.worker.SyncWorker
import javax.inject.Inject

@HiltAndroidApp
class MoneyAppApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Bound by the WorkerFactoryModule of androidx.hilt:hilt-work;
    // HiltWorkerFactory extends WorkerFactory and knows about all @HiltWorker classes.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Journal fatal crashes (Settings → «О приложении» → share log).
        ru.moneyapp.util.CrashLog.install(this)

        if (BuildConfig.DEBUG || System.getenv("MONEYAPP_DEBUG_LOG") != null) {
            Timber.plant(Timber.DebugTree())
        }

        // v1.9.0: restore the in-app language (RU/EN) before the first frame;
        // L10n.lang is in-memory snapshot state, RU is the default.
        applicationScope.launch {
            try {
                L10n.lang = if (settingsRepository.language.first() == "en") AppLang.EN else AppLang.RU
            } catch (e: Exception) {
                Timber.w(e, "Could not restore app language, keeping RU")
            }
        }

        // Seed default categories on first launch
        applicationScope.launch {
            try {
                val existing = categoryRepository.getAllAsync()
                if (existing.isEmpty()) {
                    categoryRepository.seedDefaults()
                    Timber.d("Default categories seeded")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error seeding categories")
            }
        }

        // Resume periodic Google sync if the user is connected
        applicationScope.launch {
            try {
                if (settingsRepository.googleConnected.first()) {
                    SyncWorker.enqueueSync(this@MoneyAppApplication)
                    Timber.d("Periodic Google sync resumed")
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not resume periodic sync")
            }
        }
    }

    /**
     * WorkManager configuration: workers are Hilt-injected through the
     * WorkerFactory provided by the hilt-work library (@HiltWorker).
     *
     * WorkManager 2.10 removed TaskExecutorInitializer — setTaskExecutor()
     * must NOT be called.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
}
