package ru.moneyapp.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*
import ru.moneyapp.data.auth.OAuth2TokenResponse

interface OAuth2Service {
    companion object {
        const val BASE_URL = "https://oauth2.googleapis.com/"
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val SPREADSHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
        // v1.8.0: drive.file → drive.readonly. "drive.file" only lets the app
        // touch files IT created — the user's own existing spreadsheet would
        // be 403. Read-only metadata is enough to open the linked sheet; all
        // reads/writes of the sheet itself go through the spreadsheets scope.
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        const val REDIRECT_URI = "moneyapp://oauth2-callback"
    }

    // PKCE (RFC 7636): since 2025 Google requires it for all OAuth 2.0
    // clients, including the "installed" (desktop) type. No client_secret is
    // sent — possession of the code_verifier proves legitimacy.
    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCode(
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code_verifier") codeVerifier: String
    ): OAuth2TokenResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): OAuth2TokenResponse
}
