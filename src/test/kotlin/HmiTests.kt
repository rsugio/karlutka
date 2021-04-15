import k1.GeneralQueryRequest
import k1.HmInstance
import k1.MappingTool
import k1.QueryResult
import kotlin.test.Test

class HmiTests {
    @Test
    fun generalQueryRequest() {
        val wksp = GeneralQueryRequest(
            types = GeneralQueryRequest.Types(mutableListOf(GeneralQueryRequest.Type("workspace"))),
            qc = GeneralQueryRequest.QC("S", "N",
                GeneralQueryRequest.ClCxt("L"),
                GeneralQueryRequest.SwcListDef("A")
            ),
            condition = GeneralQueryRequest.Condition(
                complex = null,
                elementary = GeneralQueryRequest.Elementary(
                    GeneralQueryRequest.Single("WS_ORDER",
                        GeneralQueryRequest.Val(GeneralQueryRequest.Simple(null, -1)))
                )
            ),
            result = GeneralQueryRequest.Result(attrib = mutableListOf("TEXT", "NAME")),
        )
        println(wksp.compose(true))
    }

    @Test
    fun generalQueryResponse() {
        val s = javaClass.getResourceAsStream("/Hmi/unescaped/qr1.xml")!!.reader().readText()
        println(QueryResult.parseUnescapedXml(s))
    }

    @Test
    fun askWksp() {
        val clientId = "f62de1959d9b11ebb68900059a3c7a00"
//        val inst = HmInstance.request(clientId, "EN", null)
    }
}