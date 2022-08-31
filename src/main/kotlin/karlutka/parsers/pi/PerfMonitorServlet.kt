package karlutka.parsers.pi

import karlutka.models.MPI
import karlutka.util.KtorClient
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

// /mdt/performancedataqueryservlet
class PerfMonitorServlet {
    companion object {
        // Mixed content для Entry требует, чтобы строки мешались с MeasuringPoints
        private val xmlmodule = SerializersModule {
            polymorphic(Any::class) {
                subclass(PerformanceDataQueryResults._MeasuringPoints::class, serializer())
                subclass(String::class, String.serializer())
            }
        }

        private val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = true
        }

        /**
         * Варианты форматов времени:
         * 2022-07-29 16:00:00.0            7.4
         * 2022-07-28T10:30:00.000+03:00    7.5
         */
        private fun _conv(s: String): TemporalAccessor {
            val fmts = listOf(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
            )
            for (f in fmts) {
                try {
                    return (f.parse(s))
                } catch (_: DateTimeParseException) {
                }
            }
            error("Нет подходящего шаблона разбора даты для `${s}'")
        }
    }

    class PerformanceTableRow(
        val INBOUND_CHANNEL: String,
        val OUTBOUND_CHANNEL: String,
        val DIRECTION: MPI.DIRECTION,
        val DELIVERY_SEMANTICS: String, //BE, EO, EOIO или пусто для аномалий
        val SERVER_NODE: String,
        val FROM_PARTY_NAME: String,
        val FROM_SERVICE_NAME: String,
        val TO_PARTY_NAME: String,
        val TO_SERVICE_NAME: String,
        val ACTION_NAME: String,
        val ACTION_TYPE: String,
        val SCENARIO_IDENTIFIER: String,
//    val TSTMSG: String,   //нет в PI 7.4 и смысла нет тащить в 7.5
        val MESSAGE_COUNTER: Long,
        val MAX_MESSAGE_SIZE: Long,
        val MIN_MESSAGE_SIZE: Long,
        val AVG_MESSAGE_SIZE: Long,
        val MAX_RETRY_COUNTER: Long,
        val MIN_RETRY_COUNTER: Long,
        val AVG_RETRY_COUNTER: String,
        val AVG_PROCESSING_TIME: Long,
        val TOTAL_PROCESSING_TIME: Long,
        val stages: MutableList<Stages>,
    ) {
        init {
            require(listOf("EO", "EOIO", "BE", "").contains(DELIVERY_SEMANTICS))
        }

        /**
         * В статистике есть аномалии, например могут быть почти пустые записи с каким-то временем
         * Для начала в них нет EO/EOIO/BE и нет интерфейсов
         */
        fun anomaly() = DELIVERY_SEMANTICS.isEmpty()

    }

    /**
     * Затраты времени на модуль - MS:module_in:CallSapAdapter, 2, 908, 563, 401
     */
    class Stages(val Name: String, val Sequence: Int, val Max: Long, val Avg: Long, val Min: Long)


    @Serializable
    @XmlSerialName("PerformanceDataQueryResults", "", "")
    class PerformanceDataQueryResults(
        @XmlElement(true)
        val Result: _Result,
        @XmlElement(true)
        val UsageTypes: _UsageTypes?,
        @XmlElement(true)
        val XIComponents: _XIComponents?,
        @XmlElement(true)
        val Periods: _Periods?,
        @XmlElement(true)
        val Data: _Data?,
    ) {
        @Serializable
        @XmlSerialName("Result", "", "")
        class _Result(
            @XmlElement(true)
            val Code: String,
            @XmlElement(true)
            val Details: String,
            @XmlElement(true)
            val Text: String,
            @XmlElement(true)
            val Component: String?,
            @XmlElement(true)
            val PeriodType: String?,
            @XmlElement(true)
            @XmlSerialName("BeginTime", "", "")
            val BeginTime: _Time?,
            @XmlElement(true)
            @XmlSerialName("EndTime", "", "")
            val EndTime: _Time?,
        )

        @Serializable
        class _Time(
            val timezone: String,
            @XmlValue(true)
            val value: String,
        ) {
            fun inst(): ZonedDateTime {
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
                val ldt = LocalDateTime.from(dtf.parse(value))
                val zid = ZoneId.of(timezone)
                val zdt = ldt.atZone(zid)
                return zdt
            }
        }

        @Serializable
        @XmlSerialName("UsageTypes", "", "")
        class _UsageTypes(
            val value: MutableList<_Usage> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Usage", "", "")
        class _Usage(
            @XmlElement(true)
            val URL: String,
            @XmlElement(true)
            val Description: String,
        )

        @Serializable
        @XmlSerialName("XIComponents", "", "")
        class _XIComponents(
            @XmlSerialName("Component", "", "")
            val value: MutableList<String> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Periods", "", "")
        class _Periods(
            val value: MutableList<_Period> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Period", "", "")
        class _Period(
            @XmlElement(true)
            val Type: String,
            @XmlElement(true)
            val Interval: MutableList<_Interval> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Interval", "", "")
        class _Interval(
            @XmlElement(true)
            val Begin: String,
            @XmlElement(true)
            val End: String,
        ) {
            @Transient
            var ta_begin: TemporalAccessor? = null

            @Transient
            var ta_end: TemporalAccessor? = null

            init {
                ta_begin = _conv(Begin)
                ta_end = _conv(End)
            }
        }

        @Serializable
        @XmlSerialName("Data", "", "")
        class _Data(
            @XmlElement(true)
            val ColumnNames: _ColumnNames,
            @XmlElement(true)
            val DataRows: _DataRows,
        )

        @Serializable
        @XmlSerialName("ColumnNames", "", "")
        class _ColumnNames(
            @XmlSerialName("Column", "", "")
            val value: MutableList<String> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("DataRows", "", "")
        class _DataRows(
//        @XmlSerialName("Row", "", "")
            val value: MutableList<_Row> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Row", "", "")
        class _Row(
//        @XmlSerialName("Entry", "", "")
            val value: MutableList<_Entry> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("Entry", "", "")
        class _Entry(
            @XmlValue(true) val data: List<@Polymorphic Any> = listOf(),
        ) {
            /**
             * Возвращает только строку, пригодно лишь для Entry без вложенных тегов
             */
            fun text(): String {
                require(data.size < 2, { "Entry должно быть без вложенных тегов" })
                if (data.size == 0)
                    return ""
                else {
                    val x = data[0]
                    require(x is String)
                    return x
                }
            }

            /**
             * Возвращает замеры
             * //TODO переписать функционально
             */
            fun mplist(): MutableList<_MP> {
                for (x in data) {
                    if (x is _MeasuringPoints) {
                        return x.value
                    }
                }
                error("Entry не содержит MeasuringPoints")
            }
        }

        @Serializable
        @XmlSerialName("MeasuringPoints", "", "")
        class _MeasuringPoints(
            @XmlElement(true)
            @XmlSerialName("MP", "", "")
            val value: MutableList<_MP> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("MP", "", "")
        class _MP(
            @XmlElement(true) val Name: String,
            @XmlElement(true) val Sequence: Int,
            @XmlElement(true) val Max: Long,
            @XmlElement(true) val Avg: Long,
            @XmlElement(true) val Min: Long,
        )

        companion object {
            @Deprecated("только для тестов")
            fun parse(sxml: String) = xmlserializer.decodeFromString<PerformanceDataQueryResults>(sxml)

            fun parse(xmlReader: XmlReader) = xmlserializer.decodeFromReader<PerformanceDataQueryResults>(xmlReader)
            fun parse(task: KtorClient.Task): PerformanceDataQueryResults {
                val o = xmlserializer.decodeFromReader<PerformanceDataQueryResults>(task.bodyAsXmlReader())
                task.close()
                return o
            }
        }

        fun render() = xmlserializer.encodeToString(this)

        /**
         * Из текста делает структурную статистику
         */
        fun perfdata(): MutableList<PerformanceTableRow> {
            val rez = mutableListOf<PerformanceTableRow>()
            if (Data == null)
                return rez
            require(Data.ColumnNames.value.size > 0) // 22 для PI 7.4, 23 для PO 7.5
            val mx = mutableMapOf<String, Int>()
            var ix = 0
            Data.ColumnNames.value.forEach { k ->
                mx[k] = ix++
            }
            require(mx["MEASURING_POINTS"] != null, { "В таблице должны быть замеры" })

            Data.DataRows.value.forEach { d ->
                val l = d.value
                val _MeasuringPoints = l[mx["MEASURING_POINTS"]!!].mplist()
                val measures = mutableListOf<Stages>()
                _MeasuringPoints.forEach { m ->
                    measures.add(Stages(m.Name, m.Sequence, m.Max, m.Avg, m.Min))
                }
                val x = PerformanceTableRow(
                    l[mx["INBOUND_CHANNEL"]!!].text(),
                    l[mx["OUTBOUND_CHANNEL"]!!].text(),
                    MPI.DIRECTION.valueOf(l[mx["DIRECTION"]!!].text()),
                    l[mx["DELIVERY_SEMANTICS"]!!].text(), //XI_DELIVERY_SEMANTICS.valueOf(l[3].text()),
                    l[mx["SERVER_NODE"]!!].text(),
                    l[mx["FROM_PARTY_NAME"]!!].text(),
                    l[mx["FROM_SERVICE_NAME"]!!].text(),
                    l[mx["TO_PARTY_NAME"]!!].text(),
                    l[mx["TO_SERVICE_NAME"]!!].text(),
                    l[mx["ACTION_NAME"]!!].text(),
                    l[mx["ACTION_TYPE"]!!].text(),
                    l[mx["SCENARIO_IDENTIFIER"]!!].text(),
                    l[mx["MESSAGE_COUNTER"]!!].text().toLong(),
                    l[mx["MAX_MESSAGE_SIZE"]!!].text().toLong(),
                    l[mx["MIN_MESSAGE_SIZE"]!!].text().toLong(),
                    l[mx["AVG_MESSAGE_SIZE"]!!].text().toLong(),
                    l[mx["MAX_RETRY_COUNTER"]!!].text().toLong(),
                    l[mx["MIN_RETRY_COUNTER"]!!].text().toLong(),
                    l[mx["AVG_RETRY_COUNTER"]!!].text(),
                    l[mx["AVG_PROCESSING_TIME"]!!].text().toLong(),
                    l[mx["TOTAL_PROCESSING_TIME"]!!].text().toLong(),
                    measures
                )
                rez.add(x)
            }
            return rez
        }

        /**
         * Вызывается только из первого (пустого) пейлоада, и там должен быть XIComponents
         */
        fun components(): MutableList<String> {
            require(XIComponents != null)
            return XIComponents.value
        }
    }

    /**
     * Разбор /mdt/messageoverviewqueryservlet
     * Не очень полезный класс но пусть будет
     */
    @Serializable
    @XmlSerialName("MessageStatisticsQueryResults", "", "")
    class MessageStatisticsQueryResults(
        @XmlElement(true)
        val Result: PerformanceDataQueryResults._Result,
        @XmlElement(true)
        val UsageTypes: PerformanceDataQueryResults._UsageTypes?,
        @XmlElement(true)
        val Views: _Views?,
        @XmlElement(true)
        val XIComponents: PerformanceDataQueryResults._XIComponents?,
        @XmlElement(true)
        val Periods: PerformanceDataQueryResults._Periods?,
        @XmlElement(true)
        val Data: PerformanceDataQueryResults._Data?,
    ) {
        @Serializable
        @XmlSerialName("Views", "", "")
        class _Views(
            @XmlElement(true)
            @XmlSerialName("View", "", "")
            val value: MutableList<_View> = mutableListOf(),
        )

        @Serializable
        class _View(
            @XmlElement(true)
            val NameKey: String,
            @XmlElement(true)
            val LocalizedName: String,
        )

        companion object {
            @Deprecated("Только для тестов")
            fun parse(sxml: String) = xmlserializer.decodeFromString<MessageStatisticsQueryResults>(sxml)

            fun parse(task: KtorClient.Task): MessageStatisticsQueryResults {
                val o = xmlserializer.decodeFromReader<MessageStatisticsQueryResults>(task.bodyAsXmlReader())
                task.close()
                return o
            }

            fun parse(xmlReader: XmlReader): MessageStatisticsQueryResults {
                return xmlserializer.decodeFromReader<MessageStatisticsQueryResults>(xmlReader)
            }
        }
    }

}