import KT.Companion.s
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.Hm.*
import kotlin.test.Test

class KHmiTests {
    @Test
    fun request() {
        val req = HmiRequest(
            "requestId", true, "serviceId", "methodId",
            HmiMethodInput(LinkedHashMap(mapOf("user_alias" to "aasasa"))),
            null,
            null,
            "clientId",
            ApplCompLevel(),
            "user", "password",
            "EN",
            0, "1.0"
        )
        req.encodeToString()

        val instResp = Hm.parseInstance(s("pi_HMI/01resp.xml"))
        val resp = HmiResponse.from(instResp)
        require(resp.MethodOutput!!.ContentType == "text/xml")

        val exc = HmiResponse.from(Hm.parseInstance(s("pi_HMI/exception.xml")))
        require(exc.CoreException!!.LocalizedMessage.startsWith("Internal error: Method generic"))

        val mm1 = HmiResponse.from(Hm.parseInstance(s("pi_HMI/mmtest_response1.xml")))
        require(mm1.MethodOutput!!.Return.length > 10000) //длинный ответ

        val om2 = HmiResponse.from(Hm.parseInstance(s("pi_HMI/omtest_response2.xml")))
        require(om2.MethodOutput!!.Return.length > 100)

        val x1 = HmiResponse.from(Hm.parseInstance(s("pi_HMI/rep_gqresp_text.xml")))
        require(x1.MethodOutput!!.Return.length > 100)

        val x2 = HmiResponse.from(Hm.parseInstance(s("pi_HMI/rep_gqresp_wksp.xml")))
        require(x2.MethodOutput!!.Return.length > 10000) //длинный ответ

        val x3 = HmiResponse.from(Hm.parseInstance(s("pi_HMI/rep_query_1.xml")))
        require(x3.MethodOutput!!.Return.length > 10000)
    }
}