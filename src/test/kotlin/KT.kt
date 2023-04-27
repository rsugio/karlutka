import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import org.apache.commons.io.input.BOMInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream


class KT {
    companion object {
        // Переключатели, какие тесты запускать
        const val testInflux = false
        const val testBtpCf = false

        /**
         * Строка из ресурса (короткое имя для тестов)
         */
        fun s(s: String): String {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) { "Ресурс $s не найден" }
            return InputStreamReader(BOMInputStream(x), Charsets.UTF_8).readText()
        }

        fun ris(s: String): InputStream {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) { "Ресурс $s не найден" }
            return x
        }

        // XmlReader из ресурса
        fun x(s: String): XmlReader {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) { "Ресурс $s не найден" }
            return PlatformXmlReader(InputStreamReader(BOMInputStream(x), Charsets.UTF_8))
        }

        // XmlReader из пути
        fun x(p: Path): XmlReader {
            require(p.exists())
            return PlatformXmlReader(InputStreamReader(p.inputStream(), Charsets.UTF_8))
        }

        // Читает тройку url, login, passw и делает двойку
        fun propAuth(p: Path): Map<String, Any> {
            require(Files.isRegularFile(p), {"$p must be a regular file"})
            val prop = Properties()
            prop.load(p.inputStream())
            val url = prop.get("url") as String
            val auth = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(prop.get("login") as String, (prop.get("passw") as String).toCharArray())
                }
            }
            return mapOf("url" to url, "auth" to auth)
        }
    }
}