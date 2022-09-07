import KT.Companion.s
import karlutka.models.MPI
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.Hm.Companion.parseInstance
import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.Zatupka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.adaptivity.xmlutil.PlatformXmlReader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.reader
import kotlin.io.path.writeText
import kotlin.test.Test

class KXiObjTests {
    @Test
    fun repository() {
        val swc = MPI.Swcv("3f38b2400b9e11ea9c32fae8ac130d0e", "zz", "?", "?", 'S', "EN", "?")
        val namespdecl = XiObj.decodeFromString(s("/pi_xiObj/rep01namespdecl.xml")).toNamespaces(swc)
        require(namespdecl.isNotEmpty())

        val ad = XiObj.decodeFromString(s("/pi_xiObj/rep02adaptermeta.xml"))
        require(!ad.content.isEmpty)
        val mmap = XiObj.decodeFromString(s("/pi_xiObj/rep03mmap.xml"))
        println(mmap)
        val om = XiObj.decodeFromString(s("/pi_xiObj/rep04om.xml"))
        println(om)
    }

    @Test
    fun xi_trafo() {
        XiObj.decodeFromString(s("/pi_xiObj/rep03mmap.xml")).content
    }

    @Test
    fun mass() {
        var ix = 10000000
        runBlocking {
            withContext(Dispatchers.Default) {
                Paths.get("C:\\data\\notbad").forEachDirectoryEntry("*.xml") { p ->
                    val xr = PlatformXmlReader(p.reader())
                    val hr = Hm.HmiResponse.from(parseInstance(xr))
                    try {
                        val xo = XiObj.decodeFromString(hr.MethodOutput!!.Return)
                        val type = xo.idInfo.key.typeID
                        Paths.get("c:\\data\\tmp\\${ix}_$type.xml").writeText(hr.MethodOutput!!.Return)
                        if (xo.documentation!=null) println(xo.documentation)
                    } catch (e: Exception) {
                        System.err.println(p)
                        e.printStackTrace()
                    }
                    ix++
                }
            }
        }
    }

    @Test
    fun tpt() {
        val p = Paths.get(javaClass.getResource("/pi_Tpz/XI7_1_SAP_ABA_7.50-sp22.tpt")!!.toURI())
        val tmp = Files.createTempFile("tpt", ".bin")
        println(tmp)
        Zatupka.unpage(p, tmp)
        Zatupka.list(tmp)
    }
}