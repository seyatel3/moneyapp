package ru.moneyapp.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (RFC 7636) helper, S256 challenge method.
 *
 * Google requires PKCE for all OAuth 2.0 clients since 2025, including the
 * "installed" (desktop) type used by this app. The verifier is stored in
 * [TokenStorage] between the auth redirect and the token exchange.
 */
object PkceUtil {

    data class Challenge(
        val verifier: String,
        val challenge: String
    )

    fun generate(): Challenge {
        val random = SecureRandom()
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return Challenge(verifier, challenge)
    }
}
