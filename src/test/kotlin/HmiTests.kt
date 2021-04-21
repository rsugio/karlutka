import k1.*
import kotlin.test.Test

class HmiTests {

    /**
     * выдача HMI-запроса в удобном для разработки виде
     */
    @Test
    fun mixed() {
        // один из вариантов запроса -- через Map.Pair, многословный
        val x = HmInstance.ofMap("com.sap.aii.util.hmi.core.msg.HmiRequest", mapOf(
            Pair("ClientId", "f62de1959d9b11ebb68900059a3c7a00"),
            Pair("ClientLanguage", "EN"),
            Pair("ClientLevel",
                HmInstance.ofMap("com.sap.aii.util.applcomp.ApplCompLevel", mapOf(
                    Pair("Release", "7.0"),
                    Pair("SupportPackage", "0"),
                ))),
            Pair("ClientPassword", "ClientPassword"),
            Pair("ClientUser", "uname"),
            Pair("ClientLanguage", "EN"),
        )
        )

        val qu = GeneralQueryRequest.ofArg(listOf("workspace"),
            GeneralQueryRequest.elementary("WS_ORDER", "EQ", GeneralQueryRequest.Simple(-1)),
            "", "RA_WORKSPACE_ID", "WS_NAME")

        // через String vararg пропроще
        val y = HmInstance.ofArg(
            "com.sap.aii.util.hmi.core.msg.HmiRequest",
            "ClientId", "f62de1959d9b11ebb68900059a3c7a00",
            "ClientLanguage", "EN",
            "ClientLevel",
            HmInstance.ofArg(
                "com.sap.aii.util.applcomp.ApplCompLevel",
                "Release", "7.0",
                "SupportPackage", "0",
            ),
            "ClientPassword", "dummy",
            "ClientUser", "uname",
            "ControlFlag", "0",
            "HmiSpecVersion", "1.0",
            "MethodId", "GENERIC",
            "MethodInput",
            HmInstance.ofArg(
                "com.sap.aii.util.hmi.api.HmiMethodInput",
                "Parameters",
                HmInstance.ofArg("com.sap.aii.util.hmi.core.gdi2.EntryStringString",
                    "Key", "QUERY_REQUEST_XML",
                    "Value", qu.compose()
                )
            ),
            "RequestId", "fb3c98b19d9b11ebbc2a00059a3c7a00",
            "RequiresSession", "false",
            "ServerApplicationId", null,
            "ServerLogicalSystemName", null,
            "ServiceId", "QUERY",
        )
        // Это запрос в {{host}}/rep/query/int?container=any
        println(y.printXml())
        // Это сырой ответ:
        val respXml = javaClass.getResourceAsStream("/Hmi/rep_gqresp_wksp.xml")!!.reader().readText()
        val resphmi = HmInstance.parse(respXml)
        val queryResultXml = resphmi.get("MethodOutput")?.get("Return")?.value?.get(0) as String?
        requireNotNull(queryResultXml)
//        println(queryResultXml)
        val res = QueryResult.parseUnescapedXml(queryResultXml).toTable()
        println(res)
    }

    /**
     * Выдача только квери в общем виде. Проще через GeneralQueryRequest.ofArg который и надо развивать.
     */
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
                        GeneralQueryRequest.Val(GeneralQueryRequest.Simple(-1)), "EQ")
                )
            ),
            result = GeneralQueryRequest.Result(attrib = mutableListOf("TEXT", "NAME")),
        )
        println(wksp.compose())
    }

    @Test
    fun generalQueryResponse() {
        val s = javaClass.getResourceAsStream("/Hmi/unescaped/rep_gqresp_wksp.xml")!!.reader().readText()
        val res = QueryResult.parseUnescapedXml(s)
        val table = res.toTable()
        println(table)
    }

    @Test
    fun testMapping() {
        val s = javaClass.getResourceAsStream("/Hmi/unescaped/testExecutionRequest.xml")!!.reader().readText()
        val ter = TestExecutionRequest.parse(s)
        println(ter.testData.inputXml)
        val t = TestExecutionRequest(
            TestExecutionRequest.Ref(
                TestExecutionRequest.VC("guid", "S"),
                TestExecutionRequest.Key("MAPPING", mutableListOf("A", "B"))
            ), TestExecutionRequest.TestData("&lt;a/&gt;",
                TestExecutionRequest.Parameters(
                    TestExecutionRequest.TestParameterInfo(
                        TestExecutionRequest.HIParameters(
                            TestExecutionRequest.Properties(
                                mutableListOf(TestExecutionRequest.Property("name", "value"))
                            )), TestExecutionRequest.HIParameters(TestExecutionRequest.Properties(mutableListOf(
                            TestExecutionRequest.Property("name", "value")))))
                ),
                TestExecutionRequest.TestParameters("AAA", 1, 2),
                -1)
        )
        if (false) println(t.composeXml())
    }

}