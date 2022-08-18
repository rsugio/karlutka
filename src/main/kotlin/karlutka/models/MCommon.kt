package karlutka.models

import io.ktor.client.plugins.auth.providers.*
import kotlinx.serialization.Serializable

class MCommon {
    @Serializable
    data class AuthToken(
        val access_token: String,
        val token_type: String,
        val expires_in: Int,
        val scope: String = "",
        val jti: String = "",
    ) {
        fun auth() = "$token_type $access_token"
        fun bearer() = BearerTokens(access_token, "")
    }
}