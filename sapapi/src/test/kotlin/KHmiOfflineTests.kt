import KT.Companion.s
import KT.Companion.x
import org.junit.jupiter.api.Tag
import ru.rsug.karlutka.pi.Hmi
import ru.rsug.karlutka.pi.HmiUsages
import ru.rsug.karlutka.pi.MPI
import ru.rsug.karlutka.pi.PCommon
import java.io.StringWriter
import java.nio.file.Files
import kotlin.io.path.outputStream
import kotlin.test.Test

@Tag("Offline")
class KHmiOfflineTests {
    @Test
    fun parsehmi() {
        return
        var services = HmiUsages.decodeHmiServicesFromReader(x("/pi_HMI/rep_registered.xml"))
        require(services.list.size == 170)
        services = HmiUsages.decodeHmiServicesFromResponse(Hmi.HmiResponse(Hmi.decodeInstanceFromReader(x("/pi_HMI/rep_registered2.xml"))))
        require(services.list.size == 170)
        require(HmiUsages.decodeHmiServicesFromReader(x("/pi_HMI/rep_supportedmethods.xml")).list.size==160)

        var instance = Hmi.decodeInstanceFromReader(x("/pi_HMI/response.xml"))
        var resp = Hmi.HmiResponse(instance)
        val sw = StringWriter()
        instance.encodeToWriter(sw)
        sw.close()
        require(resp.MethodOutputContentType == "text/xml")
        require(resp.MethodOutputReturn!!.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        require(resp.methodOutputTypeId == "com.sap.aii.utilxi.hmi.api.HmiMethodOutput")

        instance = Hmi.decodeInstanceFromReader(x("/pi_HMI/exception.xml"))
        resp = Hmi.HmiResponse(instance)
        requireNotNull(resp.CoreException)
        instance = Hmi.decodeInstanceFromReader(x("/pi_HMI/exception2.xml"))
        resp = Hmi.HmiResponse(instance)
        requireNotNull(resp.CoreException)

        instance = Hmi.decodeInstanceFromReader(x("/pi_HMI/request.xml"))   //1 параметр
        var req = Hmi.HmiRequest(instance)
        require(req.MethodInput == mapOf("user_alias" to "aasasa"))
        instance = Hmi.decodeInstanceFromReader(x("/pi_HMI/request4.xml"))
        req = Hmi.HmiRequest(instance)
        require(req.MethodInput == mapOf("release" to "7.0", "body" to "", "VC" to "SWC", "SP" to "-1", "UC" to "true"))

        req = Hmi.HmiRequest(Hmi.decodeInstanceFromReader(x("/pi_HMI/mmtest_request1.xml")))
        require(req.MethodInput!!.isNotEmpty() && req.methodInputTypeId!=null)
        resp = Hmi.HmiResponse(Hmi.decodeInstanceFromReader(x("/pi_HMI/mmtest_response1.xml")))
        require(resp.MethodOutputReturn!!.isNotBlank())

        req = Hmi.HmiRequest(Hmi.decodeInstanceFromReader(x("/pi_HMI/omtest_request2.xml")))
        require(req.MethodInput!!.isNotEmpty() && req.methodInputTypeId!=null)
        resp = Hmi.HmiResponse(Hmi.decodeInstanceFromReader(x("/pi_HMI/omtest_response2.xml")))
        require(resp.MethodOutputReturn!!.isNotBlank())

        /*
        val om2 = Hm.parseResponse(s("/pi_HMI/omtest_response2.xml"))
        require(om2.MethodOutput!!.Return.length > 100)

        val x1 = Hm.parseResponse(s("/pi_HMI/rep_gqresp_text.xml"))
        require(x1.MethodOutput!!.Return.length > 100)

        val x2 = Hm.parseResponse(s("/pi_HMI/rep_gqresp_wksp.xml"))
        require(x2.MethodOutput!!.Return.length > 10000)                //длинный ответ

        val x3 = Hm.parseResponse(s("/pi_HMI/rep_query_1.xml"))
        require(x3.MethodOutput!!.Return.length > 10000)
         */
    }

    @Test
    fun compose() {
        val r0 = Hmi.HmiRequest(Hmi.typeIdAiiHmiRequest,
            "1",
            "2",
            HmiUsages.ApplCompLevel("*", "*"),
            Hmi.typeIdAiiHmiInput,
            mapOf("release" to "7.5", "body" to ""),
            "DEFAULT",
            "getregisteredhmimethods"
        )
        r0.toInstance().encodeToString()
    }

    @Test
    fun queryservice() {
//        val q1 = SimpleQuery.RResult.parse(s("/pi_HMI/queryResult_swcv1.xml")).toSwcv() //DPH
//        require(q1.size == 481)
//        val q2 = SimpleQuery.RResult.parse(s("/pi_HMI/queryResult_swcv2.xml")).toSwcv()
//        require(q2.size > 100)
////        val t = Hm.hmserializer.decodeFromString<HmUsages.QueryResult>(s("/pi_HMI/namespaceResponse2.xml")).toNamespace(q1)
////        require(t.isNotEmpty())
//
//        val types = SimpleQuery.RResult.parse(s("/pi_HMI/rep1_query_resp.xml"))
//        types.toList(q1)
    }

    @Test
    fun namespace() {
//        val req1 = Hm.hmserializer.decodeFromString<HmUsages.GeneralQueryRequest>(s("/pi_HMI/namespace.xml"))
//        require(req1.result.attrib.isNotEmpty())
//
//        val req2 = HmUsages.GeneralQueryRequest.namespaces(listOf("b82055b0897311e6b783c9af0aa2b0df"))
//        require(req2.result.attrib.isNotEmpty())
//
//        val q1 = HmUsages.QueryResult.parse(s("/pi_HMI/queryResult_swcv1.xml")).toSwcv() //DPH
//        Hm.hmserializer.decodeFromString<HmUsages.QueryResult>(s("/pi_HMI/namespaceResponse.xml")).toList(q1)
    }

    @Test
    fun omtest() {
        return
        HmiUsages.TestExecutionRequest.decodeFromString(s("/pi_HMI/unescaped/testExecutionRequest.xml"))
        val r3 = HmiUsages.TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response3.xml"))
        r3.messages!!.message
        val r4 = HmiUsages.TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response4.xml"))
        r4.exception!!.message.contentString.trim()
    }

    @Test
    fun _dirConfiguration() {
        return
        val conf = HmiUsages.DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration.xml"))
        require(conf.FEATURES.FEATURE.size == 30)
        val conf2 = HmiUsages.DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration2.xml"))
        require(conf2.FEATURES.FEATURE.size == 30)
    }

    @Test
    fun _read() {
        return
        val ref = HmiUsages.Ref(
            PCommon.VC('S', "3f38b2400b9e11ea9c32fae8ac130d0e", -1),
            PCommon.Key(MPI.ETypeID.namespdecl, "00000000000000000000000000000000", listOf("3f38b2400b9e11ea9c32fae8ac130d0e"))
        )
        val type = HmiUsages.Type(
            "namespdecl", ref,
            ADD_IFR_PROPERTIES = true,
            STOP_ON_FIRST_ERROR = false,
            RELEASE = "7.0",
            DOCU_LANG = "EN"
        )
        val list = HmiUsages.ReadListRequest(type)
        require(list.type.ADD_IFR_PROPERTIES)
        //println(list.encodeToString())
    }

    @Test
    fun v2ParseNPrint() {
        return
        val nulos = Files.createTempFile("null_","*.xml").outputStream()
        var i: Hmi.Instance
        var r: Hmi.HmiRequest
        i = Hmi.decodeInstanceFromReader(x("/pi_HMI/request.xml"))
        require(i.attributes.size == 14)
        i.encodeToStream(nulos)
        r = i.toHmiRequest()
        require(r.MethodInput!!.size == 1)

        i = Hmi.decodeInstanceFromReader(x("/pi_HMI/hmi01req.xml"))
        require(i.attributes.size == 2)
        i.encodeToStream(nulos)

        i = Hmi.decodeInstanceFromReader(x("/pi_HMI/03many.xml"))
        require(i.attributes[0].value.size == 5)
        i.encodeToStream(nulos)

        i = Hmi.decodeInstanceFromReader(x("/pi_HMI/request4.xml"))
        require(i.attributes.size > 10)
        i.encodeToStream(nulos)
        r = i.toHmiRequest()
        require(r.MethodInput!!.size == 5)
    }

    @Test
    fun cpaCache() {
        return
        val i = Hmi.decodeInstanceFromReader(x("/pi_AE/rwb02select.xml"))

        val partyReq = Hmi.decodeInstanceFromReader(x("/pi_AE/hmi02cpaParty_req.xml")).toHmiRequest()
        val partyResp = partyReq.copyToResponse("text/plain", "")
        println(partyResp)
        partyResp.toInstance().encodeToStream(System.out)
        println()
    }
}