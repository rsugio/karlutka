import KT.Companion.s
import KT.Companion.x
import karlutka.models.MCamelDSL
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
        val cpa = XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa07.xml"))
        cpa.AllInOne.forEach { ico ->
            val ico = ico.toParsed(cpa)
            println(ico.senderAttr)
            println(ico.receivers)
            println()
            ico.channelsR.forEach { println(it) }
            println()
            ico.condgroups.forEach { println(it) }
            //val r = MCamelDSL.Route("0#корованы", MCamelDSL.From("timer:thief?period=10s"))
        }
    }

    @Test
    fun compose() {
        val r = MCamelDSL.Route("0#корованы", MCamelDSL.From("timer:thief?period=10s"))
        r.add(MCamelDSL.Description("умнО работать это хорошо"))
        r.add(MCamelDSL.Log("------- Граблю корованы -------"))
        val w1 = MCamelDSL.When(MCamelDSL.Predicate.XPath("/person/city = 'Москва'"))
        w1.add(MCamelDSL.Log("Москва"))
        w1.add(MCamelDSL.To("file:/camel/Москва"))
        val c1 = MCamelDSL.Choice()
        c1.whens.add(w1)
        c1.otherwise.add(MCamelDSL.Log("НеМосква"))
        c1.otherwise.add(MCamelDSL.To("file:/camel/НеМосква"))
        r.add(c1)
        println(r.encodeToString())
    }
}