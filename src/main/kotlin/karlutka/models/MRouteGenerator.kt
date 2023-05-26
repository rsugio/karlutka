package karlutka.models

import karlutka.parsers.pi.XICache
import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext

class MRouteGenerator(val ico: XICache.AllInOneParsed) {
    val log = StringBuilder()   //лог принятия решения

    fun endpointFrom(fromUrl: String): Endpoint {
        val camelContext: CamelContext = DefaultCamelContext(false)
        camelContext.addRoutes(object : RouteBuilder() {
            @Throws(Exception::class)
            override fun configure() {
                from(fromUrl)
            }
        })
        return camelContext.getEndpoint(fromUrl)
    }

    fun endpointTo(toUrl: String): Endpoint {
        val camelContext: CamelContext = DefaultCamelContext(false)
        camelContext.addRoutes(object : RouteBuilder() {
            override fun configure() {
                from("file:/tmp")
                    .to(toUrl)
            }
        })
        return camelContext.getEndpoint(toUrl)
    }

    fun convertIco(): String {
        require(ico.receivers.isNotEmpty())
        val fromUrl = ico.senderAttr.find { it.Name == "connection" && it.Namespace == "camel" }!!.valueAsString()
        val fromEndpoint = endpointFrom(fromUrl)
        val mep = fromEndpoint.exchangePattern
        val processor = ico.senderAttr.find { it.Name == "processor" && it.Namespace == "camel" }!!.valueAsString()
        val processor2 = ico.senderAttr.find { it.Name == "processor2" && it.Namespace == "camel" }!!.valueAsString()
        val route = MCamelDSL.Route(ico.routeId, MCamelDSL.From(fromUrl))

        log.append(
            """ICo: ${ico.fromParty}|${ico.fromService}|{${ico.fromIfacens}}${ico.fromIface}|${ico.toParty}|${ico.toService}
CC: ${ico.senderCC},
sender exchangePattern: $mep
"""
        )
        val descr = MCamelDSL.Description("непустой")
        route.add(descr)

        route.add(processor)
        route.add(MCamelDSL.Process("procmonFrom"))
        route.add(processor2)
//        route.add(MCamelDSL.Process("#karlutka.server.FAE.ProcMon"))
        class Tmp(
            val ConditionGroupId: String,   //guid
            val pname: String,              // вида icord##, появляется в маршруте
            val isRD: Boolean,              // условие для RD, иначе для ID, третьего не видел
            val receivers: List<Any>,
            var subroute: MCamelDSL.Route
        )

        val tmpRDs = mutableListOf<Tmp>()   // развилки по получателям
        val tmpIDs = mutableListOf<Tmp>()   // развилки по меппингам

        ico.condgroups.forEachIndexed { cx, cg ->
            val tmp = Tmp(cg.id, "icord$cx", cg.receivers.isNotEmpty(), cg.receivers,
                MCamelDSL.Route("${ico.routeId}_$cx", MCamelDSL.From("direct:${ico.routeId}_$cx")))
            if (tmp.isRD)
                tmpRDs.add(tmp)
            else
                tmpIDs.add(tmp)
            val ors = cg.condlinegroups.map { line ->
                val ands = line.cond.map { p ->
                    val apos = p.right.contains('\'')
                    val quot = p.right.contains('"')
                    val r = when {
                        apos && quot -> {
                            // Экранирование требует разбирательств с xmlutil, так как иначе &apos; справедливо
                            // превращается в &amp;apos; а костылить суррогатные замены очень ненормально
                            // Возможно надо сделать DelegatingXmlWriter под выдачу маршрутов, заодно и неймспейсы
                            // приделать
                            TODO("both ' & \" are in right part of XPath-expression, unimplemented yet")
                        }
                        apos -> "\"${p.right}\""
                        else -> "'${p.right}'"
                    }

                    when {
                        p.xpath != null && p.op == XICache.EOp.EQ -> "${p.xpath}=$r"
                        p.xpath != null && p.op == XICache.EOp.NE -> "${p.xpath}!=$r"
                        p.xpath != null && p.op == XICache.EOp.CP -> "${p.xpath}~=$r"
                        p.xpath != null && p.op == XICache.EOp.EX -> "boolean(${p.xpath})"
                        else -> {
                            require(p.cobj != null)
                            "false"  //пока не делаем
                        }
                    }
                }
                ands.joinToString(" and ")
            }
            val xp = ors.joinToString(" or ")
            if (cg.receivers.isNotEmpty()) {
                val sp = MCamelDSL.SetProperty(tmp.pname, MCamelDSL.Predicate.XPath("BOOLEAN", xp.toString()))
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
                val toUrl = recv.attrs.find { it.Name == "connection" && it.Namespace == "camel" }!!.valueAsString()
                val rproc = recv.attrs.find { it.Name == "processor" && it.Namespace == "camel" }!!.valueAsString()
                val rproc2 = recv.attrs.find { it.Name == "processor2" && it.Namespace == "camel" }!!.valueAsString()
                val rcve = endpointTo(toUrl)    //TODO переделать, добавить сюда наш сендер и генерить маршрут
                route.add(rproc)
                route.add(MCamelDSL.To(toUrl))
                route.add(MCamelDSL.SetProperty("FAEReceiver", MCamelDSL.Predicate.Simple(rn)))
                route.add(MCamelDSL.Process("procmonTo"))
                route.add(rproc2)
            } else {
                val recv = ico.receivers
                log.append("RD содержит несколько безусловных получателей - делаем .multicast(parallel)\n")
                val multi = MCamelDSL.Multicast(false)
                route.add(multi)
                ico.channelsR.forEach {recv ->
                    val rn = "${recv.receiver.party}|${recv.receiver.service}"
                    val toUrl = recv.attrs.find { it.Name == "connection" && it.Namespace == "camel" }!!.valueAsString()
                    val rproc = recv.attrs.find { it.Name == "processor" && it.Namespace == "camel" }!!.valueAsString()
                    val rproc2 = recv.attrs.find { it.Name == "processor2" && it.Namespace == "camel" }!!.valueAsString()
                    val rcve = endpointTo(toUrl)
                    //multi.children.add(rproc)
                    multi.children.add(MCamelDSL.SetProperty("FAEReceiver", MCamelDSL.Predicate.Simple(rn)))
                    multi.children.add(MCamelDSL.Process("procmonTo"))
                    multi.children.add(MCamelDSL.To(toUrl))
                    //route.add(rproc2)
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