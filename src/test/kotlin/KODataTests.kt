import KT.Companion.s
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import kotlin.test.Test

class KODataTests {
    @Test
    fun statics() {
        val e = PEdmx.parseEdmx(s("/cpiNeo/apiV1.edmx"))
        require(e.Version=="1.0")
        val users = PCpi.parse<PCpi.UserCredential>(s("/cpiNeo/UserCredentials.odata.json"))
        require(users.first.size==6 && users.second == null)
        val datastoreentries = PCpi.parse<PCpi.DataStoreEntry>(s("/cpiNeo/DataStoreEntries.odata.json"))
        require(datastoreentries.first.size>0 && datastoreentries.second=="DataStoreEntries?\$skiptoken=21476")
        println(datastoreentries.first.last())
    }
}