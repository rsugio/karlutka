import KT.Companion.x
import karlutka.parsers.pi.Cim
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
    private val po = KT.propAuth(Paths.get(".etc/po.properties"))
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    private fun op(cim: Cim.CIM): Cim.CIM {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("CIMProtocolVersion", cim.MESSAGE!!.PROTOCOLVERSION)        //1.0
            .header("CIMOperation", "MethodCall")               // странно, почему-то передаём IMETHODCALL в содержимом а в заголовке надо так
            .header("CIMMethod", cim.MESSAGE!!.SIMPLEREQ!!.IMETHODCALL!!.NAME)
            .header("CIMObject", "sld/active")      // в теории можно вытащить из параметра cim но оно пока не требуется
            .header("Content-Type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(cim.encodeToString()))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return if (response.statusCode() == 200) {
            val p = Paths.get("build/cim.xml")
            val os = p.outputStream()
            response.body().copyTo(os)
            os.close()
            val resp = Cim.decodeFromReader(PlatformXmlReader(p.inputStream(), "UTF-8"))
            if (resp.getError()!=null) {
                System.err.println(resp.getError())
            }
            resp
        } else {
            System.err.println(response)
            System.err.println(String(response.body().readAllBytes()))
            TODO()
        }
    }

    @Test
    fun assoc() {
        var x: Cim.CIM?
        x = op(SLD_CIM.SAPExt_GetObjectServer())
        val host = SLD_CIM.SAPExt_GetObjectServer_resp(x)
        println("host = $host")
        // хост + sld/active это обязательно для ассоциаций
        val namespacepath = Cim.NAMESPACEPATH(host, SLD_CIM.sldactive)

        // Делаем отдельную систему
        val system = SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toInstanceName("standalone1")
        x = SLD_CIM.createInstance(system, mapOf("Caption" to "Caption Standalone1", "Description" to "Дескрипшон"))
        x = op(x)
        //println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        // Делаем кластер
        val cluster = SLD_CIM.Classes.SAP_DotNetSystemCluster.toInstanceName("cluster1")
        x = SLD_CIM.createInstance(cluster, mapOf("Caption" to "Caption Cluster1", "Description" to "Дескрипшон Кластера1"))
        x = op(x)
        //println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        // Делаем ассоциацию от системы к кластеру
        //SLD_CIM.createInstance(x)
        x = SLD_CIM.associatorNames(system, SLD_CIM.Classes.SAP_DotNetSystemClusterDotNetSystem)
        //println(x.encodeToString())
        x = op(x)
        x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.OBJECTPATH.forEach {ox->
            //println(ox.INSTANCEPATH)
        }
        x = SLD_CIM.enumerateInstances("SAP_DotNetSystemClusterDotNetSystem")
        x = op(x)
        // SAP_DotNetSystemClusterDotNetSystem.GroupComponent=ref"SAP_DotNetSystemCluster.CreationClassName=\"SAP_DotNetSystemCluster\",Name=\"cluster1\"",PartComponent=ref"SAP_StandaloneDotNetSystem.CreationClassName=\"SAP_StandaloneDotNetSystem\",Name=\"standalone1\""
        x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE.forEach {vni ->
            //println("${vni.INSTANCENAME.KEYBINDING}")
        }

        val f1 = Cim.createPropertyReference("GroupComponent", "SAP_DotNetSystemCluster", Cim.INSTANCEPATH(namespacepath, cluster) )
        val t1 = Cim.createPropertyReference("PartComponent", "SAP_StandaloneDotNetSystem", Cim.INSTANCEPATH(namespacepath, system) )
        val ca = Cim.createAssociation(SLD_CIM.Classes.SAP_DotNetSystemClusterDotNetSystem.toString(), f1, t1)
        x = SLD_CIM.createInstance(ca)
        //println(x.encodeToString())
        x = op(x)

        // удаляем ассоциацию
        x = SLD_CIM.deleteInstance(ca)
        //x = op(x)
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        // удаляем кластер и систему

    }

    @Test
    fun runtime() {
        var x: Cim.CIM?
        x = op(SLD_CIM.SAPExt_GetObjectServer())
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        x = op(SLD_CIM.getClass(SLD_CIM.Classes.SAP_XIDomain))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        x = op(SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_StandaloneDotNetSystem, "Name", "Caption"))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
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
        x = op(SLD_CIM.referenceNames(instanceName))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        val i = instance(
            SLD_CIM.Classes.SAP_StandaloneDotNetSystem,
            mapOf("CreationClassName" to SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toString(), "Name" to "2", "Caption" to "2")
        )
        x = op(SLD_CIM.createInstance(i))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        x = op(SLD_CIM.modifyInstance(instanceName, mapOf("Caption" to "22222")))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        x = op(SLD_CIM.deleteInstance(instanceName))
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
    }

    @Suppress("UNUSED_VALUE")
    @Test
    fun parserPrinter() {
        var x: Cim.CIM
        x = Cim.decodeFromReader(x("/pi_SLD/cim.xml"))
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
        x = Cim.decodeFromReader(x("/pi_SLD/cim18associatornames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim19associatornames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim20association.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim21association.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim22deleteassociation.xml"))
    }
}