package ru.rsug.karlutka.server

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.ResourceHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rsug.karlutka.camel.RouteGenerator
import ru.rsug.karlutka.pi.MPI
import ru.rsug.karlutka.pi.XICache
import ru.rsug.karlutka.util.DB
import java.time.Instant
import java.time.ZoneId

/**
 * Класс для всего кэмельного в FAE
 */
class FAECamel(val fae: FAE) {

    private val logger: Logger = LoggerFactory.getLogger("fae.FAECamel")!!
    private val camelContext = DefaultCamelContext(true)
    val channels = mutableListOf<XICache.Channel>()
    val allinone = mutableListOf<XICache.AllInOne>()
    val routes = mutableMapOf<String, RouteGenerator>()
    val msktz: ZoneId = ZoneId.of("Europe/Moscow")

    init {
        logger.debug("FAECamel started")
        var rs = DB.executeQuery(DB.readFAE, fae.sid)
        if (!rs.next()) {
            DB.executeUpdateStrict(DB.insFAE, fae.sid, fae.afFaHostdb)
        }
        rs = DB.executeQuery(DB.readFCPAO, fae.sid)
        while (rs.next()) {
            val s = rs.getString("XML")
            when (MPI.ETypeID.valueOf(rs.getString("TYPEID"))) {
                MPI.ETypeID.Channel -> channels.add(XICache.decodeChannelFromString(s))
                MPI.ETypeID.AllInOne -> allinone.add(XICache.decodeAllInOneFromString(s))
                else -> error("Wrong typeid")
            }
        }
        camelContext.inflightRepository.isInflightBrowseEnabled = true
        camelContext.setAutoCreateComponents(true)
        camelContext.name = fae.afFaHostdb
        allinone.forEach { ico ->
            // создание или изменение
            val required = ico.getChannelsOID().associateWith { oid -> channels.find { it.ChannelObjectId == oid } }
            val missed = required.filter { it.value == null }
            if (missed.isEmpty()) {
                generateRoute(ico.toParsed(required.values.filterNotNull()))
            }
        }
    }

    fun cpalistener(cr: XICache.CacheRefresh, now: Instant) {
        // Слушаем обновления кэша
        // если объект есть в deletedObjects и в присланном кэше то это изменение, если в deletedObjects и нет в кэше то удаление
        val deletedObjects = cr.DELETED_OBJECTS?.SAPXI_OBJECT_KEY?.toMutableList() ?: mutableListOf()
        val updateList = mutableListOf<String>()    //AllInOneObjectId а не икохи, так как привязки каналов могут меняться
        cr.Channel.forEach { ch ->
            val delo = deletedObjects.find { it.OBJECT_ID == ch.ChannelObjectId }
            val s = ch.encodeToString()
            val prev = channels.find { it.ChannelObjectId == ch.ChannelObjectId }
            val rs = DB.executeQuery(DB.readFCPA, fae.sid, ch.ChannelObjectId)
            when {
                rs.next() && prev != null -> {
                    // объект существует
                    DB.executeUpdateStrict(DB.updFCPA, fae.sid, ch.ChannelObjectId, s, now.toEpochMilli())
                    channels.remove(prev)
                }

                prev == null -> {
                    // объекта нет - добавляем
                    val name = "${ch.PartyName}|${ch.ServiceName}|${ch.ChannelName}"
                    DB.executeUpdateStrict(DB.insFCPA, fae.sid, ch.ChannelObjectId, MPI.ETypeID.Channel.toString(), name, s, now.toEpochMilli())
                }

                else -> error("Channel inconsistency")
            }
            if (delo != null) deletedObjects.remove(delo)   //удаление из списка удалений
            channels.add(ch)
            // для перегенерации икох с этим каналом
            val touched = allinone.filter { it.getChannelsOID().contains(ch.ChannelObjectId) }.map { it.AllInOneObjectId }
            updateList.addAll(touched)
        }
        cr.AllInOne.forEach { ico ->
            updateList.add(ico.AllInOneObjectId)
            val delo = deletedObjects.find { it.OBJECT_ID == ico.AllInOneObjectId }
            val s = ico.encodeToString()
            val prev = allinone.find { it.AllInOneObjectId == ico.AllInOneObjectId }
            val rs = DB.executeQuery(DB.readFCPA, fae.sid, ico.AllInOneObjectId)

            when {
                rs.next() && prev != null -> {
                    // икоха существует
                    DB.executeUpdateStrict(DB.updFCPA, fae.sid, ico.AllInOneObjectId, s, now.toEpochMilli())
                    allinone.remove(prev)
                }

                prev == null -> // икохи нет - добавляем
                    DB.executeUpdateStrict(
                        DB.insFCPA, fae.sid, ico.AllInOneObjectId, MPI.ETypeID.AllInOne.toString(),
                        "${ico.FromPartyName}|${ico.FromServiceName}|{${ico.FromInterfaceNamespace}}${ico.FromInterfaceName}" +
                                "|${ico.ToPartyName}|${ico.ToServiceName}", s, now.toEpochMilli()
                    )

                else -> error("AllInOne inconsistency")
            }
            allinone.add(ico)
            if (delo != null) deletedObjects.remove(delo)
        }
        deletedObjects.forEach { dd ->
            if (dd.OBJECT_TYPE in listOf(MPI.ETypeID.Channel, MPI.ETypeID.AllInOne)) {
                val deleted = DB.executeUpdate(DB.delFCPA, fae.sid, dd.OBJECT_ID)
                println("Delete FCPA object: ${fae.sid} ${dd.OBJECT_TYPE} ${dd.OBJECT_ID}, rowsdeleted=$deleted")
            }
        }
        // updateList - список идентификаторов икох для перегенерации, среди них могут быть удалённые
        updateList.distinct().forEach { id ->
            val ico = allinone.find { it.AllInOneObjectId == id }
            val required = ico?.getChannelsOID()?.associateWith { oid -> channels.find { it.ChannelObjectId == oid } } ?: mapOf()
            val missed = required.filter { it.value == null }
            when {
                ico != null && missed.isEmpty() -> {
                    // создание или изменение
                    generateRoute(ico.toParsed(required.values.filterNotNull()))
                }

                ico != null -> {
                    System.err.println("Нет всех требуемых каналов для икохи, OIDs=${missed.keys}")
                }

                else -> {
                    // икоха уже удалена, выводим в лог
                    TODO("икоха удалена: $id, требуется удалить маршрут")
                }
            }
        }
    }

    private fun generateRoute(parsed: XICache.AllInOneParsed) {
        val rg = RouteGenerator(parsed)
        routes[parsed.routeId] = rg
        rg.xmlDsl = rg.convertIco()
        val resource = ResourceHelper.fromString("memory.xml", rg.xmlDsl)
        val builder = XmlRoutesBuilderLoader().loadRoutesBuilder(resource) as RouteBuilder
        camelContext.addRoutes(builder)
        camelContext.start()
    }
}