package karlutka.models

import karlutka.parsers.pi.XICache
import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext

class MRouteGenerator(val ico: XICache.AllInOneParsed) {
    val log = StringBuilder()   //лог принятия решения
    val camelSender = ico.senderAttr.filter{it.Namespace=="camel"}.associate{Pair(it.Name, it.valueAsString())}

    private fun endpointFrom(fromUrl: String): Endpoint {
        val camelContext: CamelContext = DefaultCamelContext(false)
        camelContext.addRoutes(object : RouteBuilder() {
            @Throws(Exception::class)
            override fun configure() {
                from(fromUrl)
            }
        })
        return camelContext.getEndpoint(fromUrl)
    }

    fun convertIco(): String {
        require(ico.receivers.isNotEmpty())
        val fromUrl = camelSender["connection"]!!
        val fromEndpoint = endpointFrom(fromUrl)
        val mep = fromEndpoint.exchangePattern
        val descr = MCamelDSL.Description()
        val route = MCamelDSL.Route(ico.routeId, descr)
        route.add(MCamelDSL.From(fromUrl, true, ico.key))

        log.append("ICo: $ico.key,\nCC: ${ico.senderCC},\nsender exchangePattern: $mep")
        class Tmp(
            val ConditionGroupId: String,   //guid
            val pname: String,              // вида icord##, появляется в маршруте
            val isRD: Boolean,              // условие для RD, иначе для ID, третьего не видел
            val receivers: List<Any>,
            var route: MCamelDSL.Route   // для нескольких получателей
        )

        val tmpRDs = mutableListOf<Tmp>()   // развилки по получателям
        val tmpIDs = mutableListOf<Tmp>()   // развилки по меппингам

        ico.condgroups.forEachIndexed { cx, cg ->
            val tmp = Tmp(cg.id, "icord$cx", cg.receivers.isNotEmpty(), cg.receivers,
                MCamelDSL.Route("${ico.routeId}_$cx", MCamelDSL.Description("непустой")))
            tmp.route.add(MCamelDSL.From("direct:${ico.routeId}_$cx"))
            if (tmp.isRD)
                tmpRDs.add(tmp)
            else
                tmpIDs.add(tmp)
            val ors = cg.condlinegroups.map { line ->
                val ands = line.cond.map { it.xpath() }
                ands.joinToString(" and ")
            }
            val xp = ors.joinToString(" or ")
            if (cg.receivers.isNotEmpty()) {
                val sp = MCamelDSL.SetProperty(tmp.pname, MCamelDSL.Predicate.XPath("BOOLEAN", xp))
                route.add(sp)
            }
        }

        // Собственно превращение RD в маршрут
        //TODO
        // 1. учёт если MEP по маршруту меняется
        // 2. Полное тестовое покрытие
        // 3. Вставить везде меппинги-копировщики для отладки, правильно ли генерируется
        if (tmpRDs.isEmpty()) {
            if (ico.receivers.size == 1) {
                val recv = ico.channelsR[0]
                val rn = "${recv.receiver.party}|${recv.receiver.service}"
                log.append("RD содержит одного безусловного получателя $rn - делаем простой .to()\n")
                route.add(MCamelDSL.To(recv.camel["connection"]!!))
            } else {
                log.append("RD содержит несколько безусловных получателей - делаем .multicast(parallel)\n")
                val multi = MCamelDSL.Multicast(false)
                route.add(multi)
                ico.channelsR.forEach {recv ->
                    val rn = "${recv.receiver.party}|${recv.receiver.service}"
                    multi.children.add(MCamelDSL.To(recv.camel["connection"]!!))
                }
            }
        } else {
            log.append("RD содержит условия - //TODO\n")
            route.add(MCamelDSL.To("log:${ico.routeId}"))
        }
        descr.s = log.toString()
        return route.encodeToString(ico.namespaceMapping)
    }
}