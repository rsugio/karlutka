import org.apache.commons.io.input.BOMInputStream
import java.io.InputStreamReader

class KT {
    companion object {
        /**
         * Строка из ресурса
         */
        fun s(s: String): String {
            val x = Companion::class.java.getResourceAsStream(s)
            requireNotNull(x)
            return InputStreamReader(BOMInputStream(x), Charsets.UTF_8).readText()
        }
    }
}