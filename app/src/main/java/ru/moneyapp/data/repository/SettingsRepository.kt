package ru.moneyapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    // Keys
    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val GOOGLE_CONNECTED = booleanPreferencesKey("google_connected")
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SPREADSHEET_LINK = stringPreferencesKey("spreadsheet_link") // v1.8.0: user's own sheet (blank = app creates its own)
        val GOOGLE_CLIENT_ID = stringPreferencesKey("google_client_id")
        val GOOGLE_CLIENT_SECRET = stringPreferencesKey("google_client_secret")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode") // system / light / dark
        val LANGUAGE = stringPreferencesKey("language") // v1.9.0: "ru" / "en"
        // v1.6.0: monthly budget
        val MONTHLY_CAP_KOPECKS = longPreferencesKey("monthly_cap_kopecks") // 0 = disabled
        val CAP_LINKED_INCOME = booleanPreferencesKey("cap_linked_income")
        val CAP_INCOME_PCT = intPreferencesKey("cap_income_pct") // N% of monthly income
        val BUDGET_COPY_PREV_MONTH = booleanPreferencesKey("budget_copy_prev_month")
    }

    // Username
    val username: Flow<String> = dataStore.data
        .map { it[Keys.USERNAME] ?: "" }

    suspend fun setUsername(name: String) {
        dataStore.edit { it[Keys.USERNAME] = name }
    }

    // Currency
    val currencySymbol: Flow<String> = dataStore.data
        .map { it[Keys.CURRENCY_SYMBOL] ?: "₽" }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    // Google connected
    val googleConnected: Flow<Boolean> = dataStore.data
        .map { it[Keys.GOOGLE_CONNECTED] ?: false }

    suspend fun setGoogleConnected(connected: Boolean) {
        dataStore.edit { it[Keys.GOOGLE_CONNECTED] = connected }
    }

    // Spreadsheet ID
    val spreadsheetId: Flow<String> = dataStore.data
        .map { it[Keys.SPREADSHEET_ID] ?: "" }

    suspend fun setSpreadsheetId(id: String) {
        dataStore.edit { it[Keys.SPREADSHEET_ID] = id }
    }

    // v1.8.0: link to the user's own spreadsheet; blank = the app creates one
    val spreadsheetLink: Flow<String> = dataStore.data
        .map { it[Keys.SPREADSHEET_LINK] ?: "" }

    suspend fun setSpreadsheetLink(link: String) {
        dataStore.edit { it[Keys.SPREADSHEET_LINK] = link }
    }

    // Google Client ID
    val googleClientId: Flow<String> = dataStore.data
        .map { it[Keys.GOOGLE_CLIENT_ID] ?: "" }

    suspend fun setGoogleClientId(id: String) {
        dataStore.edit { it[Keys.GOOGLE_CLIENT_ID] = id }
    }

    // Google Client Secret
    val googleClientSecret: Flow<String> = dataStore.data
        .map { it[Keys.GOOGLE_CLIENT_SECRET] ?: "" }

    suspend fun setGoogleClientSecret(secret: String) {
        dataStore.edit { it[Keys.GOOGLE_CLIENT_SECRET] = secret }
    }

    // Daily reminder
    val dailyReminderEnabled: Flow<Boolean> = dataStore.data
        .map { it[Keys.DAILY_REMINDER_ENABLED] ?: false }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DAILY_REMINDER_ENABLED] = enabled }
    }

    val dailyReminderHour: Flow<Int> = dataStore.data
        .map { it[Keys.DAILY_REMINDER_HOUR] ?: 21 }

    val dailyReminderMinute: Flow<Int> = dataStore.data
        .map { it[Keys.DAILY_REMINDER_MINUTE] ?: 0 }

    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.DAILY_REMINDER_HOUR] = hour
            it[Keys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    // Theme
    val themeMode: Flow<String> = dataStore.data
        .map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    // v1.9.0: in-app language ("ru" / "en"), default RU
    val language: Flow<String> = dataStore.data
        .map { it[Keys.LANGUAGE] ?: "ru" }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    // v1.6.0: monthly budget
    val monthlyCapKopecks: Flow<Long> = dataStore.data
        .map { it[Keys.MONTHLY_CAP_KOPECKS] ?: 0L }

    suspend fun setMonthlyCapKopecks(kopecks: Long) {
        dataStore.edit { it[Keys.MONTHLY_CAP_KOPECKS] = kopecks }
    }

    val capLinkedIncome: Flow<Boolean> = dataStore.data
        .map { it[Keys.CAP_LINKED_INCOME] ?: false }

    suspend fun setCapLinkedIncome(linked: Boolean) {
        dataStore.edit { it[Keys.CAP_LINKED_INCOME] = linked }
    }

    val capIncomePct: Flow<Int> = dataStore.data
        .map { it[Keys.CAP_INCOME_PCT] ?: 100 }

    suspend fun setCapIncomePct(pct: Int) {
        dataStore.edit { it[Keys.CAP_INCOME_PCT] = pct }
    }

    val budgetCopyPrevMonth: Flow<Boolean> = dataStore.data
        .map { it[Keys.BUDGET_COPY_PREV_MONTH] ?: false }

    suspend fun setBudgetCopyPrevMonth(copy: Boolean) {
        dataStore.edit { it[Keys.BUDGET_COPY_PREV_MONTH] = copy }
    }
}
