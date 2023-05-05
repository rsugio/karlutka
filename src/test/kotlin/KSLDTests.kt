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
import kotlin.io.path.writeText

@Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
class KSLDTests {
    private val po = KT.propAuth(Paths.get(".etc/po.properties"))
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    private fun op(cim: Cim.CIM): Cim.CIM {
        val q = Paths.get("tmp/cimReq.xml")
        q.writeText(cim.encodeToString())
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("CIMProtocolVersion", cim.MESSAGE!!.PROTOCOLVERSION)        //1.0
            .header("CIMOperation", "MethodCall")               // странно, почему-то передаём IMETHODCALL в содержимом а в заголовке надо так
            .header("CIMMethod", cim.MESSAGE!!.SIMPLEREQ!!.IMETHODCALL!!.NAME)
            .header("CIMObject", "sld/active")      // в теории можно вытащить из параметра cim но оно пока не требуется
            .header("Content-Type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofFile(q))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return if (response.statusCode() == 200) {
            val p = Paths.get("tmp/cim.xml")
            val os = p.outputStream()
            response.body().copyTo(os)
            os.close()
            val resp = Cim.decodeFromReader(PlatformXmlReader(p.inputStream(), "UTF-8"))
            if (resp.getError()!=null) {
                System.err.println(resp.getError())
            } else {
                println(resp.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
            }
            resp
        } else {
            System.err.println(response)
            System.err.println(String(response.body().readAllBytes()))
            TODO()
        }
    }

    @Test
    fun AAE() {
        val _afname = "af.fa0.fake0db"
        val _af2 = "fa0.fake0db"
        val _rwbname = "rwb.$_af2"
        val _dirname = "directory.$_af2"
        val _sid = "FA0"
        val INTEGRATION_ENGINE_JAVA_NCAE_FA0 = "INTEGRATION_ENGINE_JAVA_NCAE_$_sid"

        val httpsNone = "https://none:443"
        val httpShort = po["fa0httpshort"] as String // "http://host:80"
        val httpLong = po["fa0httplong"] as String // "http://host.domain:80"

        val _admintoolname = "AdminTool.$_afname"
        val _admintoolcaption = "AdminTool of $_afname"
        val _cacherefreshname = "CacheRefresh.$_afname"
        val _cacherefreshcaption = "CacheRefresh of $_afname"
        val _runtimecheckname = "RuntimeCheck.$_afname"
        val _runtimecheckcaption = "RuntimeCheck of $_afname"

        val _portBasicUrlName = "basicURLs"
        val _portBasicUrlCaption = "Basic URLs of Adapter Engine $_afname"
        val _portAdminToolName = "port.AdminTool.$_afname"
        val _portAdminToolCaption = "Port for AdminTool of $_afname"
        val _portCacheRefreshName = "port.CacheRefresh.$_afname"
        val _portCacheRefreshCaption = "Port for CacheRefresh of $_afname"
        val _portRTCName = "port.RuntimeCheck.$_afname"
        val _portRTCCaption = "Port for RuntimeCheck of $_afname"
//        val _portBSCName = "BusinessSystemConfiguration"
//        val _portBSCCaption = "BusinessSystemConfiguration for $INTEGRATION_ENGINE_JAVA_NCAE_FA0"

        var x: Cim.CIM?
        // получаем хост
        val host = SLD_CIM.SAPExt_GetObjectServer_resp(op(SLD_CIM.SAPExt_GetObjectServer()))
        val namespacepath = Cim.NAMESPACEPATH(host, SLD_CIM.sldactive)

        // создаём пустой SAP_XIAdapterFramework
        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(_afname)
        x = SLD_CIM.createInstance(afname, mapOf("Caption" to "Adapter Engine on $_af2")) //, "Roles" to "BOUND_TO_DOMAIN\n"))
        x = op(x)

        //rwb
        val rwbname = SLD_CIM.Classes.SAP_XIRuntimeManagementServer.toInstanceName2(_rwbname)
        x = SLD_CIM.createInstance(rwbname, mapOf("Caption" to "RWB on $_af2")) //, "Roles" to "BOUND_TO_DOMAIN\n"))
        x = op(x)

        val INTEGRATION_ENGINE_JAVA_NCAE_FA0_name = SLD_CIM.Classes.SAP_BusinessSystem.toInstanceName2(INTEGRATION_ENGINE_JAVA_NCAE_FA0)
        x = SLD_CIM.createInstance(INTEGRATION_ENGINE_JAVA_NCAE_FA0_name, mapOf("Caption" to INTEGRATION_ENGINE_JAVA_NCAE_FA0)) //, "Roles" to "Integration Server\n"))
        x = op(x)

        // создаём три сервиса
        val admintoolname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, _admintoolname)
        x = SLD_CIM.createInstance(admintoolname, mapOf("Caption" to _admintoolcaption, "Purpose" to "AdminTool"))
        x = op(x)
        val cacherefreshname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, _cacherefreshname)
        x = SLD_CIM.createInstance(cacherefreshname, mapOf("Caption" to _cacherefreshcaption, "Purpose" to "CacheRefresh"))
        x = op(x)
        val RTCname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, _runtimecheckname)
        x = SLD_CIM.createInstance(RTCname, mapOf("Caption" to _runtimecheckcaption, "Purpose" to "RuntimeCheck"))
        x = op(x)

        // создаём http-порты
        val portbasicurlname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, _portBasicUrlName)
        x = SLD_CIM.createInstance(portbasicurlname, mapOf("Caption" to _portBasicUrlCaption, "Protocol" to "http", "SecureURL" to httpShort, "URL" to httpShort))
        x = op(x)
        val portadmintoolname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, _portAdminToolName)
        x = SLD_CIM.createInstance(portadmintoolname, mapOf("Caption" to _portAdminToolCaption, "Protocol" to "http", "SecureURL" to "$httpShort/mdt", "URL" to "$httpShort/mdt"))
        x = op(x)
        val portcacherefreshname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, _portCacheRefreshName)
        x = SLD_CIM.createInstance(portcacherefreshname, mapOf("Caption" to _portCacheRefreshCaption, "Protocol" to "http", "SecureURL" to "$httpShort/CPACache/invalidate", "URL" to "$httpShort/CPACache/invalidate"))
        x = op(x)
        val portRTCname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, _portRTCName)
        x = SLD_CIM.createInstance(portRTCname, mapOf("Caption" to _portRTCCaption, "Protocol" to "http", "SecureURL" to "$httpShort/AdapterFramework/rtc", "URL" to "$httpShort/AdapterFramework/rtc"))
        x = op(x)
//        val portPipelinename = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4()
//        x = SLD_CIM.createInstance(portPipelinename, mapOf("Caption" to "Pipeline of $INTEGRATION_ENGINE_JAVA_NCAE_FA0", "Protocol" to "http", "SecureURL" to httpsNone, "URL" to "$httpShort/MessagingSystem/receive/AFW/XI"))
//        x = op(x)
//        val portBSCname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(INTEGRATION_ENGINE_JAVA_NCAE_FA0_name, _portBSCName)
//        x = SLD_CIM.createInstance(portRTCname, mapOf("Caption" to _portBSCCaption, "Protocol" to "http", "SecureURL" to httpsNone, "URL" to "$httpShort/unknown"))
//        x = op(x)

        val dirname = SLD_CIM.Classes.SAP_XIIntegrationDirectory.toInstanceName2(_dirname)
        x = SLD_CIM.createInstance(dirname, mapOf("Caption" to "Integration Directory on $_af2"))
        x = op(x)

        // AdminTool.af.fa0.fake0db -> XIAF
        x = op(asx("Antecedent", afname, "Dependent", admintoolname, "SAP_HostedXIRemoteAdminService", namespacepath))
        // AdminTool.af.fa0.fake0db -> HTTPport
        x = op(asx("Antecedent", admintoolname, "Dependent", portadmintoolname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath))
        // four more
        x = op(asx("Antecedent", afname, "Dependent", cacherefreshname, "SAP_HostedXIRemoteAdminService", namespacepath))
        x = op(asx("Antecedent", cacherefreshname, "Dependent", portcacherefreshname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath))
        x = op(asx("Antecedent", afname, "Dependent", RTCname, "SAP_HostedXIRemoteAdminService", namespacepath))
        x = op(asx("Antecedent", RTCname, "Dependent", portRTCname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath))
        //basic
        x = op(asx("Antecedent", afname, "Dependent", portbasicurlname, "SAP_XIAdapterHostedHTTPServicePort", namespacepath))
    }
    fun asx(prFrom: String, instFrom: Cim.INSTANCENAME, prTo: String, instTo: Cim.INSTANCENAME, assClass: String, namespacepath: Cim.NAMESPACEPATH): Cim.CIM {
        val f1 = Cim.createPropertyReference(prFrom, instFrom.CLASSNAME, Cim.INSTANCEPATH(namespacepath, instFrom) )
        val t1 = Cim.createPropertyReference(prTo, instTo.CLASSNAME, Cim.INSTANCEPATH(namespacepath, instTo) )
        val ca = Cim.createAssociation(assClass, f1, t1)
        return SLD_CIM.createInstance(ca)
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
        val system = SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toInstanceName2("standalone1")
        x = SLD_CIM.createInstance(system, mapOf("Caption" to "Caption Standalone1", "Description" to "Дескрипшон"))
        x = op(x)

        // Делаем кластер
        val cluster = SLD_CIM.Classes.SAP_DotNetSystemCluster.toInstanceName2("cluster1")
        x = SLD_CIM.createInstance(cluster, mapOf("Caption" to "Caption Cluster1", "Description" to "Дескрипшон Кластера1"))
        x = op(x)

        // создаём ассоциацию
        val f1 = Cim.createPropertyReference("GroupComponent", "SAP_DotNetSystemCluster", Cim.INSTANCEPATH(namespacepath, cluster) )
        val t1 = Cim.createPropertyReference("PartComponent", "SAP_StandaloneDotNetSystem", Cim.INSTANCEPATH(namespacepath, system) )
        val ca = Cim.createAssociation(SLD_CIM.Classes.SAP_DotNetSystemClusterDotNetSystem.toString(), f1, t1)
        x = SLD_CIM.createInstance(ca)
        x = op(x)

        // Читаем ассоциацию от системы к кластеру #1
        x = SLD_CIM.associatorNames(system, SLD_CIM.Classes.SAP_DotNetSystemClusterDotNetSystem)
        x = op(x)
        x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.OBJECTPATH.forEach {ox->
            println(ox.INSTANCEPATH)
        }
        // второй вариант чтения ассоциаций, прямо через класс
        x = SLD_CIM.enumerateInstances("SAP_DotNetSystemClusterDotNetSystem")
        x = op(x)
        x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE.forEach {vni ->
            println("${vni.INSTANCENAME.KEYBINDING}")
        }

        // удаляем ассоциацию
        x = SLD_CIM.deleteInstance(ca)
        x = op(x)
        println(x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)

        // удаляем кластер и систему

    }

    @Test
    fun tmp() {
        op(SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_XISubSystem))
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
        val instanceName = SLD_CIM.Classes.SAP_StandaloneDotNetSystem.toInstanceName2("2")
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
        x = SLD_CIM.modifyInstance(SLD_CIM.Classes.SAP_XIDomain.toInstanceName2("1"), mapOf("Description" to "Azaza"))
        x.encodeToString()
        x = Cim.decodeFromReader(x("/pi_SLD/cim18associatornames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim19associatornames.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim20association.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim21association.xml"))         // created association instance response
        x = Cim.decodeFromReader(x("/pi_SLD/cim22deleteassociation.xml"))
        x = Cim.decodeFromReader(x("/pi_SLD/cim23createinstancechild.xml")) //PK*4 with simple instance
    }
}