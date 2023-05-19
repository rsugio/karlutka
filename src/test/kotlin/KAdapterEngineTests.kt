import KT.Companion.s
import KT.Companion.x
import karlutka.models.MCamelDSL
import karlutka.parsers.pi.XIAdapterEngineRegistration
import karlutka.parsers.pi.XICache
import nl.adaptivity.xmlutil.PlatformXmlWriter
import javax.xml.stream.XMLStreamWriter
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
        val r = MCamelDSL.Route("1", MCamelDSL.From("timer:thief?period=10s"))
        r.add(MCamelDSL.Log("------- Граблю корованы -------"))

        cpa.AllInOne.filter{it.SenderConnectivity.AdapterName=="CamelAdapter"}.forEach {ico ->

        }
    }

    @Test
    fun compose() {
        val r = MCamelDSL.Route("0#корованы", MCamelDSL.From("timer:thief?period=10s"))
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