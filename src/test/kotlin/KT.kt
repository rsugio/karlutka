import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import org.apache.commons.io.input.BOMInputStream
import java.io.InputStreamReader
import java.nio.file.Path
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
            requireNotNull(x) {"Ресурс $s не найден"}
            return InputStreamReader(BOMInputStream(x), Charsets.UTF_8).readText()
        }
        // XmlReader из ресурса
        fun x(s: String): XmlReader {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) {"Ресурс $s не найден"}
            return PlatformXmlReader(InputStreamReader(BOMInputStream(x), Charsets.UTF_8))
        }
        // XmlReader из пути
        fun x(p: Path): XmlReader {
            require(p.exists())
            return PlatformXmlReader(InputStreamReader(p.inputStream(), Charsets.UTF_8))
        }
    }
}