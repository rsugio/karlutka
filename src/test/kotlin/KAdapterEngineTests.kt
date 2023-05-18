import KT.Companion.s
import KT.Companion.x
import karlutka.parsers.pi.XIAdapterEngineRegistration
import karlutka.parsers.pi.XICache
import kotlin.test.Test

class KAdapterEngineTests {
    @Test
    fun parse() {
        XIAdapterEngineRegistration.decodeFromString(s("/pi_AE/01regtest_req.xml"))
        XIAdapterEngineRegistration.decodeFromString(s("/pi_AE/01regtest_resp.xml"))
        XIAdapterEngineRegistration.decodeFromString(s("/pi_AE/02getappdetails_req.xml"))
        XIAdapterEngineRegistration.decodeFromString(s("/pi_AE/02getappdetails_resp_full.xml"))
        XIAdapterEngineRegistration.decodeFromString(s("/pi_AE/02getappdetails_resp.xml"))
    }

    @Test
    fun cpa() {
        XICache.decodeFromReader(x("/pi_AE/cpa02.xml"))
        XICache.decodeFromReader(x("/pi_AE/cpa03.xml"))
        XICache.decodeFromReader(x("/pi_AE/cpa04.xml"))
        XICache.decodeFromReader(x("/pi_AE/cpa05.xml"))
        XICache.decodeFromReader(x("/pi_AE/cpa06.xml"))
        //XICache.decodeFromReader(x("/pi_AE/ExportedCacheUpdate.xml"))
    }

    @Test
    fun route() {
        val cpa = XICache.decodeFromReader(x("/pi_AE/cpa06.xml"))
        cpa.AllInOne.filter{it.SenderConnectivity.AdapterName=="CamelAdapter"}.forEach {ico ->
            println(ico)
        }
    }

    @Test
    fun compose() {
        //XIAdapterEngineRegistration.Scenario()
    }
}