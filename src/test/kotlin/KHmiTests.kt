import KT.Companion.s
import KT.Companion.x
import karlutka.parsers.pi.Hmi
import karlutka.parsers.pi.HmUsages
import karlutka.parsers.pi.PCommon
import org.apache.commons.io.output.NullOutputStream
import kotlin.test.Test

//TODO переписать эти тесты
class KHmiTests {
    @Test
    fun rawhmi() {
//        val services = Hm.hmserializer.decodeFromString<HmUsages.HmiServices>(s("/pi_HMI/rep_registered.xml")).list
//        require(services.size > 10)
        /*
                val inp = HmiMethodInput("ключ", "значение")
                println(inp)
                val req = HmiRequest(
                    "clientId", "requestId", ApplCompLevel(),
                    HmiMethodInput(mapOf("user_alias" to "aasasa", "body" to null, "VC" to "SWCV")),
                    "methodId",
                    "serviceId",
                    "user"
                )
                req.instance()
                //encodeToString()

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

         */
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
        HmUsages.TestExecutionRequest.decodeFromString(s("/pi_HMI/unescaped/testExecutionRequest.xml"))
        val r3 = HmUsages.TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response3.xml"))
        r3.messages!!.message
        val r4 = HmUsages.TestExecutionResponse.decodeFromString(s("/pi_HMI/omtest_response4.xml"))
        r4.exception!!.message.contentString.trim()
    }

    @Test
    fun _dirConfiguration() {
        val conf = HmUsages.DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration.xml"))
        require(conf.FEATURES.FEATURE.size == 30)
        val conf2 = HmUsages.DirConfiguration.decodeFromString(s("/pi_HMI/dir_configuration2.xml"))
        require(conf2.FEATURES.FEATURE.size == 30)
    }

    @Test
    fun _read() {
        val ref = HmUsages.Ref(
            PCommon.VC('S', "3f38b2400b9e11ea9c32fae8ac130d0e", -1),
            PCommon.Key("namespdecl", null, listOf("3f38b2400b9e11ea9c32fae8ac130d0e"))
        )
        val type = HmUsages.Type(
            "namespdecl", ref,
            ADD_IFR_PROPERTIES = true,
            STOP_ON_FIRST_ERROR = false,
            RELEASE = "7.0",
            DOCU_LANG = "EN"
        )
        val list = HmUsages.ReadListRequest(type)
        require(list.type.ADD_IFR_PROPERTIES)
        //println(list.encodeToString())
    }

    @Test
    fun v2ParseNPrint() {
        var i: Hmi.Instance
        var r: Hmi.HmiRequest
        i = Hmi.parseInstance(x("/pi_HMI/01req.xml"))
        require(i.attributes.size==14)
        i.write(NullOutputStream.NULL_OUTPUT_STREAM)
        r = i.toHmiRequest()
        require(r.MethodInput!!.size==1)

        i = Hmi.parseInstance(x("/pi_HMI/hmi01req.xml"))
        require(i.attributes.size==2)
        i.write(NullOutputStream.NULL_OUTPUT_STREAM)

        i = Hmi.parseInstance(x("/pi_HMI/03many.xml"))
        require(i.attributes[0].value.size==5)
        i.write(NullOutputStream.NULL_OUTPUT_STREAM)

        i = Hmi.parseInstance(x("/pi_HMI/04many.xml"))
        require(i.attributes.size>10)
        i.write(NullOutputStream.NULL_OUTPUT_STREAM)
        r = i.toHmiRequest()
        require(r.MethodInput!!.size==5)
    }

    @Test
    fun cpaCache() {
        val i = Hmi.parseInstance(x("/pi_AE/rwb02select.xml"))

        val partyReq = Hmi.parseInstance(x("/pi_AE/hmi02cpaParty_req.xml")).toHmiRequest()
        val partyResp = partyReq.toResponse("text/plain", "")
        println(partyResp)
        partyResp.toInstance().write(System.out)
        println()

    }

}