package karlutka.parsers.cpi

import karlutka.serialization.KZonedDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.ZonedDateTime

class PBtpNeo {
    // см /src/test/resources/btpNeo/04groups.json - в ключе только name
    @Serializable
    data class Groups(val groups: List<Map<String, String>>) {
        fun names() = groups.map { it["name"]!! }
    }

    // в ключе: name, applicationName, providerAccount
    // в ключе2: name, type, applicationRole, shared
    @Serializable
    data class Roles(val roles: List<Map<String, String>>) {
        fun names() = roles.map { it["name"]!! }
    }

    // в ключе только name
    @Serializable
    data class Users(val users: List<Map<String, String>>) {
        fun names() = users.map { it["name"]!! }
    }

    // Реализация здесь временная и по месту, не весь SCIM а просто под Neo.
    // надо это переделать под общий стандарт RFC7643
    @Serializable
    data class Scim @OptIn(ExperimentalSerializationApi::class) constructor(
        @JsonNames("resources", "Resources")
        val resources: List<ScimUser>,
        val totalResults: Int,
        val itemsPerPage: Int,
        val startIndex: Int,
        val schemas: List<String>,
    )

    @Serializable
    data class ScimUser(
        val id: String,
        val externalId: String? = null,
        val meta: ScimMeta,
        val schemas: List<String>,
        val userName: String,
        val name: ScimName? = null,
        val emails: List<ScimEmail> = listOf(),
        val roles: List<ScimRole> = listOf(),
        @SerialName("urn:sap:cloud:scim:schemas:extension:custom:2.0:UserExt")
        val UserExt: Map<String, String>,
    )

    @Serializable
    class ScimName(val familyName: String = "", val givenName: String = "")

    @Serializable
    class ScimEmail(val value: String, val primary: Boolean)

    @Serializable
    data class ScimMeta(
        @Serializable(with = KZonedDateTimeSerializer::class)
        val created: ZonedDateTime,
        @Serializable(with = KZonedDateTimeSerializer::class)
        val lastModified: ZonedDateTime,
        val location: String,
    )

    @Serializable
    data class ScimRole(
        val value: String,
        val primary: Boolean,
        val type: String,
    )
}