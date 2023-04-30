import KT.Companion.s
import karlutka.parsers.pi.XIAdapterEngineRegistration
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
    fun compose() {

        //XIAdapterEngineRegistration.Scenario()
    }
}