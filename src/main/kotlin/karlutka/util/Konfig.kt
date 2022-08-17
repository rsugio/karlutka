package karlutka.util

import com.charleskorn.kaml.*
import io.ktor.util.*
import karlutka.clients.AbapJCo
import karlutka.serialization.KPasswordSerializer
import karlutka.serialization.KPathSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.EmptySerializersModule
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.text.toCharArray

@Serializable
sealed class KfTarget {
    abstract val sid: String
    abstract val text: String?
    abstract fun loadAuths(auths: List<KfAuth>)
    abstract fun getKind(): String

    @SerialName("ABAP")
    @Serializable
    class ABAP(
        override val sid: String,
        override val text: String? = null,
        val jco: MutableMap<String, String> = mutableMapOf(),
        val auth: String = "",                                  // безопасная ссылка на аутентификацию
    ) : KfTarget() {
        override fun loadAuths(auths: List<KfAuth>) {
            val kfa = auths.find { it.id == auth }
            require(kfa != null && kfa is KfAuth.Basic)
            val props = jco.toProperties()
            AbapJCo.ZDestinationDataProvider.jcoClients[sid] = props
            props["jco.client.user"] = kfa.login
            props["jco.client.passwd"] = String(kfa.passwd())
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
    ) : KfTarget() {
        @Transient
        var basic: KfAuth.Basic? = null //TODO подумать про другие варианты аутентификации - NTLM, формы

        override fun loadAuths(auths: List<KfAuth>) {
            val kfa = auths.find { it.id == auth }
            require(kfa != null && kfa is KfAuth.Basic)
            basic = kfa
        }

        fun getBasic(): CharArray {
            requireNotNull(basic)
            val s = "Basic " + (basic!!.login + ":" + String(basic!!.passwd())).encodeBase64()
            return s.toCharArray()
        }

        override fun getKind() = "PIAF"
    }

    @SerialName("BTPNEO")
    @Serializable
    class BTPNEO(
        override val sid: String,
        override val text: String? = null,
        val technicalName: String,
    ) : KfTarget() {
        override fun loadAuths(auths: List<KfAuth>) {
            val todo = auths.find { it.id == "//TODO" }
        }

        override fun getKind() = "BTPNEO"
    }

    override fun toString() = "($sid[${getKind()}])"
}

@Serializable
data class KfInfluxDB(
    val host: String,
    val org: String,
    val bucket: String,
    val auth: String,
)

@Serializable
data class Kfg(
    val targets: MutableList<KfTarget> = mutableListOf(),
    val tmpdir: String = System.getProperty("java.io.tmpdir") + "/karlutka",
    val httpClientThreads: Int = 4,
    val httpClientConnectionTimeoutMillis: Long = 123456L,
    val httpClientRetryOnServerErrors: Int = 2,
    val httpClientLogLevel: String = "NONE",
    val httpServerListenPort: Int = 80,
    val httpServerListenAddress: String = "0.0.0.0",
    val h2connection: String = "jdbc:h2:c:/temp/h2",
    val influxdb: KfInfluxDB? = null,
) {
    fun encodeToString() = kaml.encodeToString(serializer(), this)

    companion object {
        val cfg = YamlConfiguration(
            true, true, null,
            PolymorphismStyle.Property,
            "type",
            2, 120, SequenceStyle.Block,
            SingleLineStringStyle.Plain
        )
        val kaml = Yaml(EmptySerializersModule(), cfg)

        fun parse(s: String): Kfg {
            val k = kaml.decodeFromString(serializer(), s)
            return k
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

        fun passwd() = _passwd
    }

    @SerialName("oauth")
    @Serializable
    class OAuth(
        override val id: String,
        override val text: String? = null,
        val client_id: String,
        private var client_secret: String,
        val scope: String? = null,
    ) : KfAuth() {
        fun client_secret() = client_secret.toCharArray()
    }
}

@Serializable
data class KfKeystore(
    @Serializable(with = KPathSerializer::class)
    val path: Path,
    @Serializable(with = KPasswordSerializer::class)
    val passwd: CharArray,
)

@Serializable
data class KfPasswds(
    val keystore: KfKeystore,
    val securityMaterials: MutableList<KfAuth> = mutableListOf(),
) {
    fun encodeToString() = kaml.encodeToString(serializer(), this)

    companion object {
        val cfg = YamlConfiguration(
            true, true, null,
            PolymorphismStyle.Property,
            "type",
            2, 120, SequenceStyle.Block,
            SingleLineStringStyle.Plain
        )
        val kaml = Yaml(EmptySerializersModule(), cfg)

        fun parse(s: String): KfPasswds {
            val kfauth = kaml.decodeFromString(serializer(), s)
            return kfauth
        }

        fun parse(p: Path) = parse(p.readText(Charsets.UTF_8))
    }
}