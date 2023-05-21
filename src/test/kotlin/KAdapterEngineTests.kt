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
        val cpa = XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa06.xml"))
        cpa.AllInOne.forEach { ix ->
            val ico = ix.toParsed(cpa)
            val routeId = ico.senderAttr.find { it.Name == "routeId" && it.Namespace == "camel" }!!.valueAsString()
            val fromUrl = ico.senderAttr.find { it.Name == "connection" && it.Namespace == "camel" }!!.valueAsString()
            val processor = ico.senderAttr.find { it.Name == "processor" && it.Namespace == "camel" }!!.valueAsString()
            val processor2 = ico.senderAttr.find { it.Name == "processor2" && it.Namespace == "camel" }!!.valueAsString()
            val route = MCamelDSL.Route(routeId, MCamelDSL.From(fromUrl))
            val descr = "ICo: ${ico.fromParty}|${ico.fromService}|{${ico.fromIfacens}}${ico.fromIface}|${ico.toParty}|${ico.toService}" +
                    ", CC: ${ico.senderCC}"
            route.add(MCamelDSL.Description(descr))
            println(processor)
            route.add(processor)
            route.add(processor2)
            ico.condgroups.forEachIndexed { cx, cg ->
                val pname = "icord$cx"

                val ors = cg.condlinegroups.map { line ->
                    val ands = line.cond.map { p ->
                        val apos = p.right.contains('\'')
                        val quot = p.right.contains('"')
                        val r = if (apos && quot) {
                            // Экранирование требует разбирательств с xmlutil, так как иначе &apos; справедливо
                            // превращается в &amp;apos; а костылить суррогатные замены очень ненормально
                            TODO("both ' & \" are in right part of XPath-expression, unimplemented yet")
                        } else if (apos) {
                            "\"${p.right}\""
                        } else {
                            "'${p.right}'"
                        }

                        if (p.xpath != null && p.op == XICache.EOp.EQ) {
                            "${p.xpath}=$r"
                        } else if (p.xpath != null && p.op == XICache.EOp.NE) {
                            "${p.xpath}!=$r"
                        } else if (p.xpath != null && p.op == XICache.EOp.CP) {
                            "${p.xpath}~=$r"
                        } else if (p.xpath != null && p.op == XICache.EOp.EX) {
                            "boolean(${p.xpath})"
                        } else {
                            require(p.cobj != null)
                            "false"  //пока не делаем
                        }
                    }
                    ands.joinToString(" and ")
                }
                val xp = ors.joinToString(" or ")
                if (cg.receivers.isNotEmpty()) {
                    val recvt = cg.receivers.map { "${it.party}|${it.service}" }.joinToString(",")
                    val sp = MCamelDSL.SetProperty(pname, MCamelDSL.Predicate.XPath("BOOLEAN", xp.toString()))
                    route.add(sp)
                    //r.add(MCamelDSL.Description("Receivers: $recvt"))
                }
            }
            println(route.encodeToString(ico.namespaceMapping))
        }
    }

    @Test
    fun mcameldsl() {
        var d = MCamelDSL.decodeFromString("<description>умнО работать это хорошо</description>")
        d = MCamelDSL.decodeFromString("<setBody><simple><s1:a xmlns:s1=\"urn:s1\" xmlns:s2=\"urn:s2\"><s2:b/>B</s1></simple></setBody>")
        println(d)
    }

    @Test
    fun compose() {
        val r = MCamelDSL.Route("0#корованы", MCamelDSL.From("timer:thief?period=10s"))
        r.add(MCamelDSL.Description("умнО работать это хорошо"))
        r.add(MCamelDSL.SetBody(MCamelDSL.Predicate.Simple("1")))

        r.add(MCamelDSL.Log("------- Граблю корованы -------"))
        val w1 = MCamelDSL.When(MCamelDSL.Predicate.XPath("BOOLEAN", "/s1:person/city = 'Москва'"))
        w1.add(MCamelDSL.Log("Москва"))
        w1.add(MCamelDSL.To("file:/camel/Москва"))
        val c1 = MCamelDSL.Choice()
        c1.whens.add(w1)
        c1.otherwise.add(MCamelDSL.Log("НеМосква"))
        c1.otherwise.add(MCamelDSL.To("file:/camel/НеМосква"))
        r.add(c1)
        println(r.encodeToString(mapOf("s1" to "urn:s1", "s2" to "urn:s2")))
        //TODO сделать здесь загрузку роута для валидации XML DSL
    }
}