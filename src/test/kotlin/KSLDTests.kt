import KT.Companion.x
import karlutka.parsers.pi.Cim
import karlutka.parsers.pi.Cim.CIM
import karlutka.parsers.pi.SLD_CIM
import karlutka.parsers.pi.SLD_CIM.Companion.instance
import nl.adaptivity.xmlutil.PlatformXmlReader
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.Authenticator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


class KSLDTests {
    val po = KT.propAuth(Paths.get(".etc/po.properties"))
    val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    fun op(
        cim: CIM,
        cimoperation: String = "MethodCall",
        cimobject: String = "sld/active"
    ): CIM? {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("cimprotocolversion", "1.0")
            .header("cimoperation", cimoperation)
            .header("cimmethod", cim.MESSAGE!!.SIMPLEREQ!!.IMETHODCALL!!.NAME)
            .header("cimobject", cimobject)
            .header("content-type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(cim.encodeToString()))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return if (response.statusCode() == 200) {
            val p = Paths.get("build/cim.xml")
            val os = p.outputStream()
            response.body().copyTo(os)
            os.close()
            Cim.decodeFromReader(PlatformXmlReader(p.inputStream(), "UTF-8"))
        } else {
            System.err.println(response)
            System.err.println(String(response.body().readAllBytes()))
            null
        }
    }

    @Test
    fun runtime() {
        val a1 = op(SLD_CIM.SAPExt_GetObjectServer())
        println(a1!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        val a2 = op(SLD_CIM.getClass(SLD_CIM.Classes.SAP_XIDomain))
        println(a2!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        val a3 = op(SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_StandaloneDotNetSystem, "Name", "Caption"))
        println(a3!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
//        val a4 = op(SLD_CIM.getClass("CIM_ManagedElement"))
//        println(a4!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
//        val r5 = SLD_CIM.associators(
//            "SAP_BCClient",
//            "200.SystemName.DER.SystemNumber.0021175362.SystemHome.nl-s-derdb",
//            "SAP_BusinessSystemViewedBCClient",
//            "SAP_BusinessSystem"
//        )
//        val a5 = op(r5)
//        println(a5!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        val instanceName = SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toInstanceName("2")
        val a6 = op(SLD_CIM.referenceNames(instanceName))
        println(a6!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        val i = instance(
            SLD_CIM.Classes.SAP_StandaloneDotNetSystem,
            mapOf("CreationClassName" to SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toString(), "Name" to "2", "Caption" to "2")
        )
        val a7 = op(SLD_CIM.createInstance(i))
        println(a7!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        val a8 = op(SLD_CIM.modifyInstance(instanceName, mapOf("Caption" to "22222")))
        println(a8!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
//        val a9 = op(SLD_CIM.deleteInstance(SLD_CIM.Classes.SAP_StandaloneDotNetSystem, "2"))
//        println(a9!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
    }

    @Suppress("UNUSED_VALUE")
    @Test
    fun parserPrinter() {
        var x: CIM = CIM()
        x = Cim.decodeFromReader(x("/pi_SLD/cim.xml"))
        //decodeFromStream(Paths.get("build/cim_big.xml").inputStream()) raises OOM
        x = Cim.decodeFromReader(x("/pi_SLD/cim01get.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim02get.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim03getclass.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim04getclass.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim05enuminstances.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim06enuminstances.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim07getinstance.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim08getinstance.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim09associators.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim10associators.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim11error.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim12createinstance.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim13createinstance.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim14createinstance.xml"))
        val i = instance(
            SLD_CIM.Classes.SAP_XIDomain,
            mapOf("CreationClassName" to SLD_CIM.Classes.SAP_XIDomain.toString(), "Name" to "1", "Caption" to "1")
        )
        x = SLD_CIM.createInstance(i)
        x.encodeToString()
        x = Cim.decodeFromReader(x("/pi_SLD/cim15referencenames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim16referencenames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim17modifyinstance.xml"))
        x = SLD_CIM.modifyInstance(SLD_CIM.Classes.SAP_XIDomain.toInstanceName("1"), mapOf("Description" to "Azaza"))
        x.encodeToString()
    }
}