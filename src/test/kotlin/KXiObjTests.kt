import KT.Companion.s
import karlutka.clients.Ztp
import karlutka.models.MPI
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.Hm.Companion.parseInstance
import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.Zatupka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.adaptivity.xmlutil.PlatformXmlReader
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
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
        val p = javaClass.getResourceAsStream("/pi_Tpz/XI7_1_SAP_ABA_7.50-sp22.tpt")!!
        val tmp = Files.createTempFile("tpt_", ".bin")
        println(tmp)
        Zatupka.unpage(p, tmp.outputStream())
        Zatupka.list(tmp)
    }

    fun tpz(p: Path) {
        val zf = ZipFile(p.toFile())
        val e = zf.entries().toList().find { it.name.lowercase().endsWith(".tpt") && !it.isDirectory }
        if (e==null) return
        val tmp = Files.createTempFile("tpt_", ".bin")
        Zatupka.unpage(zf.getInputStream(e), tmp.outputStream())
        val objs = Zatupka.list2(tmp)
        Files.delete(tmp)
    }

    @Test
    fun johnny() {
        Ztp.reindex(Paths.get("Y:\\Tpz"))

    }
}