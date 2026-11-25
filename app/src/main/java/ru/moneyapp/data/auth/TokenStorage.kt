package ru.moneyapp.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    suspend fun saveTokens(access: String, refresh: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, access)
            .putString(KEY_REFRESH_TOKEN, refresh)
            .apply()
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    // PKCE code_verifier: stored between the OAuth redirect and the token
    // exchange so the exchange can prove possession without a client_secret.
    suspend fun saveCodeVerifier(verifier: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    suspend fun getCodeVerifier(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_CODE_VERIFIER, null)
    }

    suspend fun clearCodeVerifier() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    suspend fun hasTokens(): Boolean = withContext(Dispatchers.IO) {
        !prefs.getString(KEY_ACCESS_TOKEN, "").isNullOrBlank() &&
                !prefs.getString(KEY_REFRESH_TOKEN, "").isNullOrBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CODE_VERIFIER = "code_verifier"
    }
}
