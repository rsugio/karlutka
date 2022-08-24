import KT.Companion.s
import karlutka.parsers.PEdmx
import kotlin.test.Test

class KEdmxTests {
    @Test
    fun a() {
        val e = PEdmx.parseEdmx(s("/cpiNeo/apiV1.edmx"))
        println(e)
    }
}