import KT.Companion.s
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import kotlin.test.Test

class KODataTests {
    @Test
    fun statics() {
        val e = PEdmx.parseEdmx(s("/cpiNeo/apiV1.edmx"))
        require(e.Version == "1.0")
        val users = PCpi.parse<PCpi.UserCredential>(s("/cpiNeo/UserCredentials.odata.json"))
        require(users.first.size == 6 && users.second == null)
        val datastoreentries = PCpi.parse<PCpi.DataStoreEntry>(s("/cpiNeo/DataStoreEntries.odata.json"))
        require(datastoreentries.first.size > 0 && datastoreentries.second == "DataStoreEntries?\$skiptoken=21476")
        val datastores = PCpi.parse<PCpi.DataStore>(s("/cpiNeo/DataStore.odata.json"))
        require(datastores.first.size > 0)
        val ipackages = PCpi.parse<PCpi.IntegrationPackage>(s("/cpiNeo/IntegrationPackages.odata.json"))
        require(ipackages.first.size > 0)
        val mpl1 = PCpi.parse<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs1.odata.json"))
        require(mpl1.first.size > 0)
        val mpl2 = PCpi.parse<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs2.odata.json"))
        require(mpl2.first.size > 0)
        require(mpl2.first[0].CustomHeaderProperties.results.size == 4)   //$expand=CustomHeaderProperties
        //одиночный лог с экспандами
        val mpl3 = PCpi.parseSingle<PCpi.MessageProcessingLog>(s("/cpiNeo/MessageProcessingLogs3.odata.json"))
        require(mpl3.CustomHeaderProperties.results[0].Name=="lifnr")
        require(mpl3.CustomHeaderProperties.results[0].Value=="11055631")
        // Артефакты по пакету
        val ipid = PCpi.parse<PCpi.IntegrationDesigntimeArtifact>(s("/cpiNeo/IntegrationPackages_IntegrationDesigntimeArtifacts.odata.json"))
        require(ipid.first.size>10)
        // просто VMG
        val vm = PCpi.parse<PCpi.ValueMappingDesigntimeArtifact>(s("/cpiNeo/ValueMappingDesigntimeArtifacts.odata.json"))
        require(vm.first.size==3)
        val se1 = PCpi.parse<PCpi.ServiceEndpoint>(s("/cpiNeo/ServiceEndpoints1.odata.json"))
        require(se1.first.size>20)
        val se2 = PCpi.parse<PCpi.ServiceEndpoint>(s("/cpiNeo/ServiceEndpoints2.odata.json"))
        require(se2.first.size>20)
        val lf = PCpi.parse<PCpi.LogFile>(s("/cpiNeo/LogFiles.odata.json"))
        require(lf.first.size>5)
        val lfa = PCpi.parse<PCpi.LogFileArchive>(s("/cpiNeo/LogFileArchives.odata.json"))
        require(lfa.first.size>3)
    }

    @Test
    fun new() {
        val lfa = PCpi.parse<PCpi.LogFileArchive>(s("/cpiNeo/LogFileArchives.odata.json"))
        println(lfa)
    }
}