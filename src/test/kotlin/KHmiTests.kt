import KT.Companion.s
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.Hm.*
import kotlin.test.Test

class KHmiTests {
    @Test
    fun rawhmi() {
        HmiMethodInput("ключ", "значение").attr()
        val req = HmiRequest(
            "clientId", "requestId", ApplCompLevel(),
            HmiMethodInput(mapOf("user_alias" to "aasasa", "body" to null, "VC" to "SWCV")),
            "methodId",
            "serviceId",
            "user",
            "password",
            "EN",
            true, null,
            null,
            "1.0", 0
        )
        println(req.encodeToString())

        val instResp = Hm.parseInstance(s("pi_HMI/01resp.xml"))
        val resp = HmiResponse.from(instResp)
        require(resp.MethodOutput!!.ContentType == "text/xml")

        val exc = HmiResponse.from(Hm.parseInstance(s("pi_HMI/exception.xml")))
        require(exc.CoreException!!.LocalizedMessage.startsWith("Internal error: Method generic"))
        Hm.parseResponse(s("pi_HMI/02exception.xml"))

        val many = Hm.parseInstance(s("pi_HMI/03many.xml"))
        require(many.attribute.size>0)

        val many4 = Hm.parseInstance(s("pi_HMI/04many.xml"))
        require(many4.attribute.size>0)

        val mm1 = Hm.parseResponse(s("pi_HMI/mmtest_response1.xml"))
        require(mm1.MethodOutput!!.Return.length > 10000) //длинный ответ

        val om2 = Hm.parseResponse(s("pi_HMI/omtest_response2.xml"))
        require(om2.MethodOutput!!.Return.length > 100)

        val x1 = Hm.parseResponse(s("pi_HMI/rep_gqresp_text.xml"))
        require(x1.MethodOutput!!.Return.length > 100)

        val x2 = Hm.parseResponse(s("pi_HMI/rep_gqresp_wksp.xml"))
        require(x2.MethodOutput!!.Return.length > 10000) //длинный ответ

        val x3 = Hm.parseResponse(s("pi_HMI/rep_query_1.xml"))
        require(x3.MethodOutput!!.Return.length > 10000)
    }

    @Test
    fun queryservice() {
        require(GeneralQueryRequest.swcv().isNotBlank())
        val q1 = QueryResult.parse(s("pi_HMI/queryResult_swcv1.xml")).toSwcv()
        require(q1.size > 100)
        val q2 = QueryResult.parse(s("pi_HMI/queryResult_swcv2.xml")).toSwcv()
        require(q2.size > 100)
    }
}