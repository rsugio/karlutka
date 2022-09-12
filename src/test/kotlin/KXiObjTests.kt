import KT.Companion.s
import karlutka.clients.Ztp
import karlutka.models.MPI
import karlutka.parsers.pi.*
import karlutka.parsers.pi.Hm.Companion.parseInstance
import karlutka.util.KTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.writeAsXML
import java.nio.file.*
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
        val s = XiObj.decodeFromString(s("/pi_xiObj/XI_TRAFO_c3f3562c66603a839cad1befc0faaa6a_6bce2b0c432911e0c8c400000fff767a"))
        XiTrafo.decodeFromString(s.content.contentString)
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

//    @Test
//    fun tpt() {
//        val p = javaClass.getResourceAsStream("/pi_Tpz/XI7_1_SAP_ABA_7.50-sp22.tpt")!!
//        val tmp = Files.createTempFile("tpt_", ".bin")
//        println(tmp)
//        Zatupka.unpage(p, tmp.outputStream())
//        Zatupka.list(tmp)
//    }

//    fun tpz(p: Path) {
//        val zf = ZipFile(p.toFile())
//        val e = zf.entries().toList().find { it.name.lowercase().endsWith(".tpt") && !it.isDirectory }
//        if (e==null) return
//        val tmp = Files.createTempFile("tpt_", ".bin")
//        val zis = zf.getInputStream(e)
//        Zatupka.unpage(zis, tmp.outputStream())
//        zis.close()
//        val objs = Zatupka.list2(tmp)
//        Files.delete(tmp)
//    }

    @Test
    fun johnny() {
        Ztp.index(Paths.get("Y:\\Tpz.old"))
    }

    @Test
    fun trafos() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/xi_trafo")!!.toURI())
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                val xo = XiObj.decodeFromPath(it)
                val tr = XiTrafo.decodeFromString(xo.content.contentString)
                if (tr.MetaData.blob != null) {
                    try {
                        val mt = tr.toMappingTool()
                    } catch (e: Exception) {
                        val ba = tr.MetaData.blob!!.content()!!
                        KTempFile.getTempFileXml("mappingtool_").writeBytes(ba)
                        System.err.println(e)
                    }
                }
            }
        }
    }

    @Test
    fun func_lib() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/func_lib")!!.toURI())
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                val xo = XiObj.decodeFromPath(it)
                val fl = FunctionLibrary.decodeFromString(xo.content.contentString)
                try {
                    fl.toFunctionStorage()
                } catch (e:Exception) {
                    KTempFile.getTempFileXml("functionstorage_").writeBytes(fl.MetaData.blob.content()!!)
                    throw e
                }

            }
        }
    }

    @Test
    fun mappingTool() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/mappingtool")?.toURI()!!)
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                val tr = MappingTool.decodeFromStream(it.inputStream())
            }
        }
    }
}