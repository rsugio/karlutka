import KT.Companion.s
import karlutka.parsers.pi.XiObj
import kotlin.test.Test

class KXiObjTests {
    @Test
    fun repository() {
        val namespdecl = XiObj.decodeFromString(s("/pi_xiObj/rep01namespdecl.xml"))
        println(namespdecl)
    }
}