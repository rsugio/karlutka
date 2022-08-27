import KT.Companion.s
import karlutka.models.MPI
import karlutka.parsers.pi.XiObj
import kotlin.test.Test

class KXiObjTests {
    @Test
    fun repository() {
        val swc = MPI.Swcv("3f38b2400b9e11ea9c32fae8ac130d0e", "zz", "?", "?", 'S', "EN", "?")
        val namespdecl = XiObj.decodeFromString(s("/pi_xiObj/rep01namespdecl.xml")).toNamespaces(swc)
        require(!namespdecl.isEmpty())

        val ad = XiObj.decodeFromString(s("/pi_xiObj/rep02adaptermeta.xml"))
        println(ad)
        val mmap = XiObj.decodeFromString(s("/pi_xiObj/rep03mmap.xml"))
        println(ad)
    }
}