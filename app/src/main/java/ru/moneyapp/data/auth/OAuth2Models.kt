package ru.moneyapp.data.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OAuth2TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String?,
    val scope: String,
    val token_type: String
)

@JsonClass(generateAdapter = true)
data class OAuth2Error(
    val error: String,
    val error_description: String?
)
