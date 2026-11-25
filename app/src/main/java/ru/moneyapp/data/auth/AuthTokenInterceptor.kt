package ru.moneyapp.data.auth

import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.Response
import ru.moneyapp.data.remote.OAuth2Service
import ru.moneyapp.data.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val oauth2Service: OAuth2Service,
    private val settingsRepository: SettingsRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenStorage.getAccessToken() }

        if (accessToken.isNullOrEmpty()) {
            return chain.proceed(chain.request())
        }

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            val refreshed = try {
                runBlocking { refreshAccessToken() }
            } catch (e: Exception) {
                return chain.proceed(chain.request())
            }

            if (refreshed != null) {
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $refreshed")
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return response
    }

    private suspend fun refreshAccessToken(): String? {
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) return null

        // The client id comes from user settings (installed client, no secret).
        val clientId = settingsRepository.googleClientId.first()
        if (clientId.isEmpty()) return null

        return try {
            val response = oauth2Service.refreshToken(refreshToken, clientId)
            tokenStorage.saveTokens(response.access_token, response.refresh_token ?: refreshToken)
            response.access_token
        } catch (e: Exception) {
            null
        }
    }
}
