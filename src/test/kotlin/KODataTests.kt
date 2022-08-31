import KT.Companion.s
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import kotlin.test.Test

class KODataTests {
    @Test
    fun errors() {
        val e1 = PCpi.parseError(s("/cpiNeo/error1.json"))
        require(e1.code=="Internal Server Error")
    }

    @Test
    fun statics() {
        // стабильные тесты
        val e = PEdmx.parseEdmx(s("/cpiNeo/apiV1.edmx"))
        require(e.Version == "1.0")
        // юзера
        val users = PCpi.parse<PCpi.UserCredential>(s("/cpiNeo/UserCredentials.odata.json"))
        require(users.first.size == 6 && users.second == null)
        // датасторы
        val datastores = PCpi.parse<PCpi.DataStore>(s("/cpiNeo/DataStore.odata.json"))
        require(datastores.first.isNotEmpty())
        // записи в датасторах
        val datastoreentries = PCpi.parse<PCpi.DataStoreEntry>(s("/cpiNeo/DataStoreEntries.odata.json"))
        require(datastoreentries.first.isNotEmpty() && datastoreentries.second == "DataStoreEntries?\$skiptoken=21476")
        // пакеты
        val ipackages = PCpi.parse<PCpi.IntegrationPackage>(s("/cpiNeo/IntegrationPackages.odata.json"))
        require(ipackages.first.isNotEmpty())
        // MPL без экспанда
        val mpl1 = PCpi.parse<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs1.odata.json"))
        require(mpl1.first.isNotEmpty())
        // MPL с $expand=CustomHeaderProperties
        val mpl2 = PCpi.parse<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs2.odata.json"))
        require(mpl2.first.isNotEmpty())
        require(mpl2.first[0].CustomHeaderProperties.results.size == 4)
        //одиночный лог с экспандами
        val mpl3 = PCpi.parseSingle<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs3.odata.json"))
        require(mpl3.CustomHeaderProperties.results[0].Name=="lifnr")
        require(mpl3.CustomHeaderProperties.results[0].Value=="11055631")
        // Артефакты по пакету
        val ipid = PCpi.parse<PCpi.IntegrationDesigntimeArtifact>(s("/cpiNeo/IntegrationPackages_IntegrationDesigntimeArtifacts.odata.json"))
        require(ipid.first.size>10)
        // VMG без пакета
        val vm = PCpi.parse<PCpi.ValueMappingDesigntimeArtifact>(s("/cpiNeo/ValueMappingDesigntimeArtifacts.odata.json"))
        require(vm.first.size==3)
        // ендпоинты
        val se1 = PCpi.parse<PCpi.ServiceEndpoint>(s("/cpiNeo/ServiceEndpoints1.odata.json"))
        require(se1.first.size>20)
        val se2 = PCpi.parse<PCpi.ServiceEndpoint>(s("/cpiNeo/ServiceEndpoints2.odata.json"))
        require(se2.first.size>20)
        // логи
        val lf = PCpi.parse<PCpi.LogFile>(s("/cpiNeo/LogFiles.odata.json"))
        require(lf.first.size>5)
        // архивы логов
        val lfa = PCpi.parse<PCpi.LogFileArchive>(s("/cpiNeo/LogFileArchives.odata.json"))
        require(lfa.first.size>3)
    }

    @Test
    fun tmp() {
        // времянки
        val lfa = PCpi.parse<PCpi.LogFileArchive>(s("/cpiNeo/LogFileArchives.odata.json"))
        println(lfa)
    }
}