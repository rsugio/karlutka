package ru.rsug.karlutka.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class PBtpCf {
    @Serializable
    class Role(
        val roleTemplateName: String,
        val roleTemplateAppId: String,
        val name: String,
        val attributeList: List<String>,
        val description: String = "",
        val scopes: List<Scope>,
        val isReadOnly: Boolean,
        val appName: String,
        val appDescription: String = "",
    )

    @Serializable
    class Scope(
        val name: String,
        val description: String = "",
        @SerialName("granted-apps")
        val granted_apps: List<String> = listOf(),
        @SerialName("grant-as-authority-to-apps")
        val grant_as_authority_to_apps: List<String> = listOf(),
    )

    @Serializable
    class RoleCollection(
        val name: String,
        val description: String = "",
        val isReadOnly: Boolean,
        val roleReferences: List<RoleReference>,
    )

    @Serializable
    class RoleReference(
        val name: String,
        val description: String = "",
        val roleTemplateAppId: String,
        val roleTemplateName: String,
    )

    @Serializable
    class App(
        val appid: String,
        val serviceinstanceid: String,
        val planId: String,
        val planName: String,
        val orgId: String,
        val spaceId: String?,
        val userName: String?,
        val description: String?,
        val masterAppId: String?,
        val tenantId: String,
        val xsappname: String,
        val attributes: List<String> = listOf(),
        @SerialName("oauth2-configuration")
        val oauth2: Oauth2Configuration,
        @SerialName("tenant-mode")
        val tenant_mode: String = "",
    )

    @Serializable
    class Oauth2Configuration(
        @SerialName("token-validity")
        val token_validity: Int,
        @SerialName("refresh-token-validity")
        val refresh_token_validity: Int,
        val autoapprove: Boolean,
        @SerialName("grant-types")
        val grant_types: List<String> = listOf(),
        @SerialName("system-attributes")
        val system_attributes: List<String> = listOf(),
        val allowedproviders: String?,
        @SerialName("redirect-uris")
        val redirect_uris: List<String> = listOf(),
        @SerialName("credential-types")
        val credential_types: List<String> = listOf(),
    )
}