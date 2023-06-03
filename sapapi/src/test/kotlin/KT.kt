import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
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

        /**
         * Строка из ресурса (короткое имя для тестов)
         */
        fun s(s: String): String {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) { "Ресурс $s не найден" }
//            return InputStreamReader(BOMInputStream(x), Charsets.UTF_8).readText()
            return InputStreamReader(x, Charsets.UTF_8).readText()
        }

        // XmlReader из ресурса
        fun x(s: String): XmlReader {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) { "Ресурс $s не найден" }
            return PlatformXmlReader(InputStreamReader(x, Charsets.UTF_8))
        }

        // XmlReader из пути
        fun x(p: Path): XmlReader {
            require(p.exists())
            return PlatformXmlReader(InputStreamReader(p.inputStream(), Charsets.UTF_8))
        }

        // Читает свойства, преобразует в Map и добавляет аутентификатор
        fun propAuth(p: Path): Map<String, Any> {
            val m = mutableMapOf<String, Any>()
            val ps = props(p)
            m.putAll(ps)
            m["auth"] = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(requireNotNull(ps["login"]), requireNotNull(ps["passw"]).toCharArray())
                }
            }
            return m
        }

        fun props(p: Path): Map<String, String> {
            require(Files.isRegularFile(p)) { "$p must be a regular file" }
            val prop = Properties()
            prop.load(p.inputStream())
            @Suppress("UNCHECKED_CAST") return prop.toMap() as Map<String, String>
        }
    }
}