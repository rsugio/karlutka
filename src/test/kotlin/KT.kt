import org.apache.commons.io.input.BOMInputStream
import java.io.InputStreamReader

class KT {
    companion object {
        /**
         * Строка из ресурса
         */
        fun s(s: String): String {
            require(s.startsWith("/"))
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x) {"Ресурс $s не найден"}
            return InputStreamReader(BOMInputStream(x), Charsets.UTF_8).readText()
        }

        const val testInflux = false
        const val testBtpCf = false
    }
}