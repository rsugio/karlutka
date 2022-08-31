import KT.Companion.s
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.Hm.*
import karlutka.parsers.pi.PCommon
import kotlinx.serialization.decodeFromString
import kotlin.test.Test

class KHmiTests {
    @Test
    fun rawhmi() {
        val services = Hm.hmserializer.decodeFromString<HmiServices>(s("/pi_HMI/rep_registered.xml")).list
        require(services.size > 10)

        HmiMethodInput("ключ", "значение").attr()
        val req = HmiRequest(
            "clientId", "requestId", ApplCompLevel(),
            HmiMethodInput(mapOf("user_alias" to "aasasa", "body" to null, "VC" to "SWCV")),
            "methodId",
            "serviceId",
            "user",
            "password",
            "EN",
            true,
            null,
            null,
            "1.0", 0
        )
        req.encodeToString()

        val instResp = Hm.parseInstance(s("/pi_HMI/01resp.xml"))
        val resp = HmiResponse.from(instResp)
        require(resp.MethodOutput!!.ContentType == "text/xml")

        val exc = HmiResponse.from(Hm.parseInstance(s("/pi_HMI/exception.xml")))
        require(exc.CoreException!!.LocalizedMessage.startsWith("Internal error: Method generic"))
        Hm.parseResponse(s("/pi_HMI/02exception.xml"))

        val many = Hm.parseInstance(s("/pi_HMI/03many.xml"))
        require(many.attribute.isNotEmpty())

        val many4 = Hm.parseInstance(s("/pi_HMI/04many.xml"))
        require(many4.attribute.isNotEmpty())

        val mm1 = Hm.parseResponse(s("/pi_HMI/mmtest_response1.xml"))
        require(mm1.MethodOutput!!.Return.length > 10000) //длинный ответ

        val om2 = Hm.parseResponse(s("/pi_HMI/omtest_response2.xml"))
        require(om2.MethodOutput!!.Return.length > 100)

        val x1 = Hm.parseResponse(s("/pi_HMI/rep_gqresp_text.xml"))
        require(x1.MethodOutput!!.Return.length > 100)

        val x2 = Hm.parseResponse(s("/pi_HMI/rep_gqresp_wksp.xml"))
        require(x2.MethodOutput!!.Return.length > 10000)                //длинный ответ

        val x3 = Hm.parseResponse(s("/pi_HMI/rep_query_1.xml"))
        require(x3.MethodOutput!!.Return.length > 10000)
    }

    @Test
    fun queryservice() {
        require(GeneralQueryRequest.swcv().isNotBlank())
        val q1 = QueryResult.parse(s("/pi_HMI/queryResult_swcv1.xml")).toSwcv() //DPH
        require(q1.size == 481)
        val q2 = QueryResult.parse(s("/pi_HMI/queryResult_swcv2.xml")).toSwcv()
        require(q2.size > 100)
        val t = Hm.hmserializer.decodeFromString<QueryResult>(s("/pi_HMI/namespaceResponse2.xml")).toNamespace(q1)
        require(t.isNotEmpty())

        val types = QueryResult.parse(s("/pi_HMI/rep1_query_resp.xml"))
        types.toTable().forEach { m ->
            val ref = m["RA_XILINK"]!!.qref!!.ref
            println(ref)
        }
    }

    @Test
    fun namespace() {
        val req1 = Hm.hmserializer.decodeFromString<GeneralQueryRequest>(s("/pi_HMI/namespace.xml"))
        require(req1.result.attrib.isNotEmpty())

        val req2 = GeneralQueryRequest.namespaces(listOf("b82055b0897311e6b783c9af0aa2b0df"))
        require(req2.result.attrib.isNotEmpty())

        Hm.hmserializer.decodeFromString<QueryResult>(s("/pi_HMI/namespaceResponse.xml")).toTable()
    }

    @Test
    fun omtest() {
        TestExecutionRequest.decodeFromString(s("/pi_HMI/unescaped/testExecutionRequest.xml"))
        val r3 = TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response3.xml"))
        println(r3.messages!!.message)
        val r4 = TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response4.xml"))
        println(r4.exception!!.message.contentString.trim())
    }

    @Test
    fun dirConfiguration() {
        val conf = DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration.xml"))
        require(conf.FEATURES.FEATURE.size == 30)
        val conf2 = DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration2.xml"))
        require(conf2.FEATURES.FEATURE.size == 30)
    }

    @Test
    fun read() {
        val ref = Ref(
            PCommon.VC("3f38b2400b9e11ea9c32fae8ac130d0e", 'S', -1),
            PCommon.Key("namespdecl", null, listOf("3f38b2400b9e11ea9c32fae8ac130d0e"))
        )
        val type = Type("namespdecl", ref,
            ADD_IFR_PROPERTIES = true,
            STOP_ON_FIRST_ERROR = false,
            RELEASE = "7.0",
            DOCU_LANG = "EN"
        )
        val list = ReadListRequest(type)
        println(list.encodeToString())
    }
}