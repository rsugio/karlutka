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
        require(users.size==6)
    }
}