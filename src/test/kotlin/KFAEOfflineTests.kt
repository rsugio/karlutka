import KT.Companion.s
import KT.Companion.x
import karlutka.models.MCamelDSL
import karlutka.parsers.pi.XIAdapterEngineRegistration
import karlutka.parsers.pi.XICache
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("Offline")
class KFAEOfflineTests {
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
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa01.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa02.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa03.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa04.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa05.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa06.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa07.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/ExportedCacheUpdate_AMD.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/ExportedCacheUpdate_ICo.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/ExportedCacheUpdate_Channel.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE/ExportedCacheUpdateFull.xml.sensitive"))
    }

    @Test
    fun parseIco() {
        val cpa = XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa06.xml"))
        cpa.AllInOne.filter { it.FromServiceName == "BC_TEST4_1234" }.forEach { ix ->
            //val xml = convertIco(ix.toParsed(cpa))
            println("------------------------------------------")
            //println(xml)
        }
    }

    @Test
    fun mcameldsl() {
        var d = MCamelDSL.decodeSetBodyFromString("<setBody><simple>A123</simple></setBody>")
        //TODO d = MCamelDSL.decodeSetBodyFromString("<setBody><simple><s1:a xmlns:s1=\"urn:s1\" xmlns:s2=\"urn:s2\"><s2:b/>B</s1></simple></setBody>")
    }

    @Test
    fun compose(): String {
        val r = MCamelDSL.Route("0#корованы", MCamelDSL.Description("умнО работать это хорошо"))
        val from = MCamelDSL.From("timer:thief?period=10s", true, "CC_A_Send", MCamelDSL.Description("|BC_TEST1|Mega_OutAsync"))
        r.add(from)
        r.add(MCamelDSL.SetBody(MCamelDSL.Predicate.Simple("1")))

        r.add(MCamelDSL.Log("------- Граблю корованы -------"))
        val w1 = MCamelDSL.When(MCamelDSL.Predicate.XPath("BOOLEAN", "/s1:person/city = 'Москва'"))
        w1.add(MCamelDSL.Log("Москва"))
        w1.add(MCamelDSL.To("file:/camel/Москва"))
        val c1 = MCamelDSL.Choice()
        c1.whens.add(w1)
        c1.otherwise.add(MCamelDSL.Log("НеМосква"))
        c1.otherwise.add(MCamelDSL.Multicast(false, MCamelDSL.To("direct:НеМосква")))
        r.add(c1)
        val s = r.encodeToString(mapOf("s1" to "urn:s1", "s2" to "urn:s2"))
        println(s)
        return s
    }
}