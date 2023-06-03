import KT.Companion.x
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import ru.rsug.karlutka.pi.XICache

@Tag("Offline")
class KXICacheTests {

    @Test
    fun cpa() {
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa01.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa02.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa03.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa04.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa05.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa06.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/cpa07.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/ExportedCacheUpdate_AMD.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/ExportedCacheUpdate_ICo.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/ExportedCacheUpdate_Channel.xml"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/ExportedCacheUpdateFull.xml.sensitive"))
        XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/run_value_mapping_cache_int_InvalidateCache_F.xml.sensitive"))
    }

    @Test
    fun parseIco() {
        // Это полный рефреш полученный {{host}}/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=T&consumer=af.fa0.fake0db
        // чем режим T и TF отличаются, непонятно
        val cpa = XICache.decodeCacheRefreshFromReader(x("/pi_AE_Cache/run_value_mapping_cache_int_InvalidateCache_F.xml.sensitive"))
        cpa.AllInOne.forEach { ico ->
            val n = ico.getChannelsOID().associateWith { oid -> cpa.Channel.find{it.ChannelObjectId==oid} }
            val nn = n.values.filterNotNull()
            require(n.size==nn.size) {"Не должно быть неизвестных каналов"}
//            val mrg = MRouteGenerator(ico.toParsed(nn))
//            println(mrg.convertIco())
        }
    }

//    @Test
//    fun mcameldsl() {
//        var d = MCamelDSL.decodeSetBodyFromString("<setBody><simple>A123</simple></setBody>")
//        //TODO d = MCamelDSL.decodeSetBodyFromString("<setBody><simple><s1:a xmlns:s1=\"urn:s1\" xmlns:s2=\"urn:s2\"><s2:b/>B</s1></simple></setBody>")
//    }

//    @Test
//    fun compose(): String {
//        val r = MCamelDSL.Route("0#корованы", MCamelDSL.Description("умнО работать это хорошо"))
//        val from = MCamelDSL.From("timer:thief?period=10s", true, "CC_A_Send", MCamelDSL.Description("|BC_TEST1|Mega_OutAsync"))
//        r.add(from)
//        r.add(MCamelDSL.SetBody(MCamelDSL.Predicate.Simple("1")))
//
//        r.add(MCamelDSL.Log("------- Граблю корованы -------"))
//        val w1 = MCamelDSL.When(MCamelDSL.Predicate.XPath("BOOLEAN", "/s1:person/city = 'Москва'"))
//        w1.add(MCamelDSL.Log("Москва"))
//        w1.add(MCamelDSL.To("file:/camel/Москва"))
//        val c1 = MCamelDSL.Choice()
//        c1.whens.add(w1)
//        c1.otherwise.add(MCamelDSL.Log("НеМосква"))
//        c1.otherwise.add(MCamelDSL.Multicast(false, MCamelDSL.To("direct:НеМосква")))
//        r.add(c1)
//        val s = r.encodeToString(mapOf("s1" to "urn:s1", "s2" to "urn:s2"))
//        println(s)
//        return s
//    }
}