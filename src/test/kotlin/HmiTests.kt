import k1.GeneralQueryRequest
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
        println(wksp.compose(false))
    }
}