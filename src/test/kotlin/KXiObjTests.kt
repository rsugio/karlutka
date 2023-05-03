import KT.Companion.s
import KT.Companion.x
import karlutka.clients.ZtpDB
import karlutka.models.MPI
import karlutka.parsers.pi.*
import karlutka.server.DB
import karlutka.server.Server
import karlutka.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.*
import kotlin.test.Test

class KXiObjTests {
    init {
        Server.kfpasswds = KfPasswds.parse(Paths.get("C:\\data\\passwd.yaml"))
        Server.kfg = Kfg.parse(Paths.get("c:\\data\\karla.yaml"))
        KKeystore.load(Server.kfpasswds.keystore.path, Server.kfpasswds.keystore.passwd)
        KTempFile.tempFolder = Paths.get(Server.kfg.tmpdir)
        DB.init(Server.kfg.h2connection)
    }

    @Test
    fun repository() {
        val swc = MPI.Swcv("3f38b2400b9e11ea9c32fae8ac130d0e", null, null, null, null, null)
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
    fun johnny() {
//      Ztp.index(Paths.get("Y:\\Tpz"))
      ZtpDB.index(Paths.get("Y:\\Tpz"))
//      DB.dot1()
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
                        tr.toMappingTool()
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
                } catch (e: Exception) {
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
                MappingTool.decodeFromStream(it.inputStream())
            }
        }
    }

    @Test
    fun sld() {
        val z = Cim.decodeFromReader(x("/pi_SLD/export17.xml"))
//        val lst = z.DECLARATION!!.DECLGROUP_WITHNAME.VALUE_NAMEDOBJECT
//            .filter { it.INSTANCE.CLASSNAME == "SAP_SoftwareComponent" }
//            .map { it.INSTANCE }
//        lst.forEach { i ->
//            val vendor = i.PROPERTY.find { it.NAME == "Vendor" }?.VALUE
//            val version = i.PROPERTY.find { it.NAME == "Version" }?.VALUE
//            val name = i.PROPERTY.find { it.NAME == "Name" }?.VALUE
//            val PPMSNumber = i.PROPERTY.find { it.NAME == "PPMSNumber" }?.VALUE
//            val caption = i.PROPERTY.find { it.NAME == "Caption" }?.VALUE
//            val description = i.PROPERTY.find { it.NAME == "Description" }?.VALUE
//            val technologyType = i.PROPERTY.find { it.NAME == "TechnologyType" }?.VALUE
//            var guid = i.PROPERTY.find { it.NAME == "GUID" }?.VALUE
//            if (guid != null) {
//                guid = guid.replace("-", "")
//                require(guid.length == 32)
//                println("$guid $vendor  $technologyType     $version\t\t$caption|$name")
//            }
//        }
    }

    @Test
    fun sld2() {
        val p = Paths.get("C:\\Temp\\2022-09-13\\sld\\SID_export_20220914_180804.zip")
        val zis = ZipInputStream(p.inputStream())
        val lst = mutableListOf<SLD_CIM.SAP_SoftwareComponent>()
        SLD_CIM.decodeFromZip(zis) { cim ->
//            cim.DECLARATION!!.DECLGROUP_WITHNAME.VALUE_NAMEDOBJECT.forEach{
//                val s = SLD_CIM.SAP_SoftwareComponent.from(it)
//                if (s!=null && s.Vendor=="sap.com") lst.add(s)
//            }
        }
        val q = p.resolveSibling("swcv.json")
        q.writeText(Json.encodeToString(lst))
    }
}