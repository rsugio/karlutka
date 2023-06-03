package ru.rsug.karlutka.util

import com.charleskorn.kaml.*
import io.ktor.client.plugins.logging.*
import io.ktor.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer
import ru.rsug.karlutka.serialization.KPasswordSerializer
import ru.rsug.karlutka.serialization.KPathSerializer
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.text.toCharArray

class Konfig {
    @Serializable
    sealed class Target {
        abstract val sid: String
        abstract val text: String?
        abstract fun loadAuths(auths: List<KfAuth>)
        abstract fun setAuth(auth: KfAuth)
        abstract fun getKind(): String

        @SerialName("ABAP")
        @Serializable
        class ABAP(
            override val sid: String,
            override val text: String? = null,
            private val jco: MutableMap<String, String> = mutableMapOf(),
            val auth: String = "",                                  // безопасная ссылка на аутентификацию
        ) : Target() {
            override fun loadAuths(auths: List<KfAuth>) {
                val kfa = auths.find { it.id == auth }
                require(kfa != null && kfa is KfAuth.Basic)
                val props = jco.toProperties()
//            AbapJCo.ZDestinationDataProvider.jcoClients[sid] = props
                props["jco.client.user"] = kfa.login
                props["jco.client.passwd"] = String(kfa.passwd())
            }

            override fun setAuth(auth: KfAuth) {
                TODO()
            }

            override fun getKind() = "ABAP"
        }

        @SerialName("PIAF")
        @Serializable
        class PIAF(
            override val sid: String,
            override val text: String? = null,
            val url: String,
            val auth: String = "",
            val checkAuthResource: String = "",
            val retries: Int = 2,
            val logLevel: LogLevel = LogLevel.INFO
        ) : Target() {
            @Transient
            var basic: KfAuth.Basic? = null //TODO подумать про аутентификацию формой /logon_ui_resources/

            override fun loadAuths(auths: List<KfAuth>) {
                setAuth(auths.find { it.id == auth }!!)
            }

            override fun setAuth(auth: KfAuth) {
                require(auth is KfAuth.Basic)
                basic = auth
            }

            override fun getKind() = "PIAF"
        }

        @SerialName("FAE")
        @Serializable
        class FAE(
            override val sid: String,
            override val text: String? = null,
            val cae: String,
            val sld: String,
            val fakehostdb: String,
            val realHostPortURI: String,
            val domain: String
        ) : Target() {
            init {
                require(cae.isNotBlank())
            }

            override fun loadAuths(auths: List<KfAuth>) {}
            override fun setAuth(auth: KfAuth) {}
            override fun getKind() = "FAE"

        }

        @SerialName("BTPNEO")
        @Serializable
        class BTPNEO(
            override val sid: String,
            override val text: String? = null,
            val globalaccount: String = "",      // CA1212121212112122323424343435
            val subaccount: String,              // не гуид а техническое имя, вида asasa20eew
            val apihost: String,                 // https://api.eu3.hana.ondemand.com
            val auth: String,
        ) : Target() {
            @Transient
            lateinit var oauth: KfAuth.OAuth     // переделать на нормальный OAuth

            override fun loadAuths(auths: List<KfAuth>) {
                val kfa = auths.find { it.id == auth }
                require(kfa != null) { "для $sid не найден пароль $auth" }
                require(kfa is KfAuth.OAuth) { "для $sid найден пароль $auth но это не OAuth" }
                oauth = kfa
            }

            override fun setAuth(auth: KfAuth) {
                TODO("Not yet implemented")
            }

            override fun getKind() = "BTPNEO"
        }

        @SerialName("BTPCF")
        @Serializable
        class BTPCF(
            override val sid: String,
            override val text: String? = null,
            val globalaccount: String = "",        // CA1212121212112122323424343435
            val subaccount: String,                // eddd1d16-2a25-4055-86c0-7405b88ea57d
//        val orgId: String = "",                // 00029bc0-34d5-4f10-9852-c91334a1bd8a подключить по мере надобности
//        val apihost: String,                   // https://api.cf.us10.hana.ondemand.com   подключить по мере надобности
            val apiAuthentication: String,         // = "https://api.authentication.us10.hana.ondemand.com",
            val auth: String = "",
        ) : Target() {
            @Transient
            lateinit var oauth: KfAuth.OAuth

            override fun loadAuths(auths: List<KfAuth>) {
                val oauth = auths.find { it.id == auth }
                require(oauth != null && oauth is KfAuth.OAuth)
                this.oauth = oauth
            }

            override fun setAuth(auth: KfAuth) {
                TODO("Not yet implemented")
            }

            override fun getKind() = "BTPCF"
        }

        @SerialName("CPINEO")
        @Serializable
        class CPINEO(
            override val sid: String,
            override val text: String? = null,
            val tmn: String,                 // https://e100999-tmn.hci.eu3.hana.ondemand.com/
//        val iflmap: String? = null,              // https://e100999-iflmap.hci.eu3.hana.ondemand.com/
            val auth: String,
        ) : Target() {
            @Transient
            var basic: KfAuth.Basic? = null

            @Transient
            var oauth: KfAuth.OAuth? = null

            override fun loadAuths(auths: List<KfAuth>) {
                val kfa = auths.find { it.id == auth }
                require(kfa != null) { "для $sid не найден пароль $auth" }
                when (kfa) {
                    is KfAuth.Basic -> {
                        basic = kfa
                    }

                    is KfAuth.OAuth -> {
                        oauth = kfa
                    }

                    else -> {
                        error("Unsupported")
                    }
                }
            }

            override fun setAuth(auth: KfAuth) {
                TODO("Not yet implemented")
            }

            override fun getKind() = "CPINEO"
        }

        override fun toString() = "($sid[${getKind()}])"
    }

    @Serializable
    class Kfg(
        // Это конфигурационный файл на вход
        val targets: MutableList<Target> = mutableListOf(),
        val tmpdir: String = System.getProperty("java.io.tmpdir") + "/karlutka",
        val httpClientThreads: Int = 4,
        val httpClientConnectionTimeoutMillis: Long = 123456L,
        val httpClientRetryOnServerErrors: Int = 2,
        val httpClientLogLevel: String = "NONE",
        val httpServerListenPort: Int = 80,
        val httpServerListenAddress: String = "0.0.0.0",
        val h2connection: String = "jdbc:h2:c:/temp/h2",
    ) {
        fun encodeToString() = kaml.encodeToString(serializer(), this)

        companion object {
            private val cfg = YamlConfiguration(
                encodeDefaults = true, strictMode = true, extensionDefinitionPrefix = null,
                polymorphismStyle = PolymorphismStyle.Property,
                polymorphismPropertyName = "type",
                encodingIndentationSize = 2, breakScalarsAt = 120, sequenceStyle = SequenceStyle.Block,
                singleLineStringStyle = SingleLineStringStyle.Plain
            )
            val kaml = Yaml(EmptySerializersModule(), cfg)

            fun parse(s: String): Kfg {
                return kaml.decodeFromString(serializer(), s)
            }

            fun parse(p: Path) = parse(p.readText(Charsets.UTF_8))
        }
    }

    @Serializable
    sealed class KfAuth {
        abstract val id: String
        abstract val text: String?

        @SerialName("apitoken")
        @Serializable
        class ApiToken(
            override val id: String,
            override val text: String? = null,
            @Serializable(with = KPasswordSerializer::class)
            val token: CharArray,
        ) : KfAuth()

        @SerialName("basic")
        @Serializable
        class Basic(
            override val id: String,
            override val text: String? = null,
            val login: String,
            private var passwd: String,
        ) : KfAuth() {
            @Transient
            private var _passwd = CharArray(0)
            fun passwd() = _passwd

            init {
                if (passwd.startsWith("!encoded!")) {
                    _passwd = CharArray(3)
                    _passwd[0] = '1'
                    _passwd[1] = '2'
                    _passwd[2] = '3'
                } else {
                    // энкодим пароль
                    _passwd = passwd.toCharArray()
                    this.passwd = "!encoded!9876"
                }
            }
        }

        @SerialName("oauth")
        @Serializable
        class OAuth(
            override val id: String,
            override val text: String? = null,
            val url: String,
            val client_id: String,
            @Serializable(with = KPasswordSerializer::class)
            val client_secret: CharArray,
        ) : KfAuth() {
            fun getBasic(): String {
                return "Basic " + (client_id + ":" + String(client_secret)).encodeBase64()
            }

        }
    }

    @Serializable
    class KfKeystore(
        @Serializable(with = KPathSerializer::class)
        val path: Path,
        @Serializable(with = KPasswordSerializer::class)
        val passwd: CharArray,
    )

    @Serializable
    class KfPasswds(
        // Это файл с паролями и секурщиной на входе
        val keystore: KfKeystore,
        val securityMaterials: MutableList<KfAuth> = mutableListOf(),
    ) {
        fun encodeToString() = kaml.encodeToString(serializer(), this)

        companion object {
            private val cfg = YamlConfiguration(
                encodeDefaults = true, strictMode = true, extensionDefinitionPrefix = null,
                polymorphismStyle = PolymorphismStyle.Property,
                polymorphismPropertyName = "type",
                encodingIndentationSize = 2, breakScalarsAt = 120, sequenceStyle = SequenceStyle.Block,
                singleLineStringStyle = SingleLineStringStyle.Plain
            )
            val kaml = Yaml(EmptySerializersModule(), cfg)

            fun parse(s: String): KfPasswds {
                return kaml.decodeFromString(serializer(), s)
            }

            fun parse(p: Path) = parse(p.readText(Charsets.UTF_8))
        }
    }
}