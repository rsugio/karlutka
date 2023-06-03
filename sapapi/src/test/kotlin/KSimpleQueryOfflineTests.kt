import KT.Companion.x
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import ru.rsug.karlutka.pi.*
import ru.rsug.karlutka.pi.SimpleQuery.EResult

@Tag("Offline")
class KSimpleQueryOfflineTests {
    @Test
    fun parsers() {
        val x = SimpleQuery.decodeQueryRequestFromReader(x("/pi_SimpleQuery/sq01req_namespaces.xml"))
        require(x.etypes == listOf(MPI.ETypeID.namespace))
        x.encodeToString()
        val y = SimpleQuery.decodeQueryResultFromReader(x("/pi_SimpleQuery/sq01resp_namespaces.xml"))
        require(y.matrix.r.size == 1)

        val nav = SimpleQuery.decodeNavigationRequestInputFromReader(x("/pi_SPROXY/navirequest01.xml"))
        require(nav.queryAttributes.attribute.size==2)
        require(nav.navigationRequest.existenceCheckOnly)
    }

    @Test
    fun usages() {
        // просто вызовы функций, которые будут в коде потом вызваны
        val dir = SimpleQuery.queryRequestDir(listOf(), listOf())
        SimpleQuery.queryRequestRep(listOf(), listOf())
        require(SimpleQuery.queryRequestDirV().qc.delMode=='N')
        SimpleQuery.RequestByNameNamespaceEQ(MPI.ETypeID.ifmoper, "", "", listOf())
        SimpleQuery.RequestByNameNamespaceEQ.fromRequest(dir)

        SimpleQuery.Complex()
        SimpleQuery.conditionWS_TYPE_S  //FESR
    }

    @Test
    fun getIcoChannelsDescriptions() {
        // создаём HMI-клиента для симпл квери (/rep или /dir, неважно)
        val hmiclient = HmiClient.simpleQueryClient("7.5", "anyuser")

        // вытаскиваем все икохи и каналы
        val simpleQueryRequest = SimpleQuery.queryRequestDirV(
            MPI.ETypeID.AllInOne, MPI.ETypeID.Channel,
            EResult.RA_XILINK, EResult.OBJECTID, EResult.TEXT
        )

        // делаем HMI-запрос в виде сперва HMI-instance а затем XML
        val hmireqxml = hmiclient.request("QUERY_REQUEST_XML", simpleQueryRequest.encodeToString()).toInstance().encodeToString()
        require(hmireqxml.isNotBlank())
        // тут запрос через postman к {{host}}/dir/query/int?container=any и в "/pi_SimpleQuery/hq01resp_texts.xml" ответ
        val response = hmiclient.parseResponse(Hmi.decodeInstanceFromReader(x("/pi_SimpleQuery/hq01resp_texts.xml")))
        // Вытаскиваем из HmiResponse.MethodOutputReturn энкоденную XML-строку и преобразуем её в SimpleQuery.QueryResult
        val rez = SimpleQuery.Companion.decodeQueryResultFromString(response.MethodOutputReturn!!)
        // для каждой строки ответа выводим тип сущности, OBJECTID и TEXT
        rez.matrix.r.forEach {
            val typeID = it.c[0].qref!!.ref.key.typeID
            val oid = it.c[1].simple!!.bin!!
            val desc = it.c[2].simple!!.strg!!
            println("$typeID\t$oid = $desc")
        }

    }
}