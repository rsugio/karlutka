import KT.Companion.s
import karlutka.models.MPI
import karlutka.models.MPerfStat
import karlutka.parsers.pi.PerfMonitorServlet.MessageStatisticsQueryResults
import karlutka.parsers.pi.PerfMonitorServlet.PerformanceDataQueryResults
import nl.adaptivity.xmlutil.PlatformXmlReader
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.Test

class KPerformanceServletTests {
    private fun show(a: Any?) {
        if (false) println(a?.toString())
    }

    @Test
    fun static() {
        val p1 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/01empty.xml"))
        show(p1.components())
        val p2 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/02intervals.xml"))
        show(p2.Periods!!.value.size)
        p2.UsageTypes!!.value.forEach {
            require(it.URL.isNotBlank())
            require(it.Description.isNotBlank())
        }
        val p3 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/03data.xml"))
        show(p3.perfdata().size)
        requireNotNull(p3.Result.BeginTime)
        requireNotNull(p3.Result.EndTime)
        val p4 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/04datamin.xml"))
        show(p4.perfdata().size)
        val p5 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/05intervals.xml"))
        show(p5.Periods!!.value.size)
        val p6 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/06nodata.xml"))
        show(p6)
        p6.perfdata().forEach { x ->
            require(x.INBOUND_CHANNEL.isNotBlank())
            require(x.OUTBOUND_CHANNEL.isNotBlank())
            require(x.DIRECTION == MPI.DIRECTION.INBOUND || x.DIRECTION == MPI.DIRECTION.OUTBOUND)
            require(x.SERVER_NODE.isNotBlank())
            require(x.ACTION_NAME.isNotBlank())
            require(x.ACTION_TYPE.isNotBlank())
            require(x.SCENARIO_IDENTIFIER.isNotBlank())
            require("${x.FROM_PARTY_NAME}${x.FROM_SERVICE_NAME}${x.TO_PARTY_NAME}${x.TO_SERVICE_NAME}".isNotBlank())
            val z = x.MESSAGE_COUNTER + x.MAX_MESSAGE_SIZE + x.MIN_MESSAGE_SIZE + x.AVG_MESSAGE_SIZE +
                    x.MAX_RETRY_COUNTER + x.MIN_RETRY_COUNTER + x.AVG_PROCESSING_TIME + x.TOTAL_PROCESSING_TIME
            require(z > 0)
            require(x.AVG_RETRY_COUNTER.isNotBlank())
            require(x.stages.isNotEmpty())
            x.stages.forEach { s ->
                require(s.Avg + s.Max + s.Min + s.Sequence > 0)
            }
        }
    }

    @Test
    fun composer() {
        val p = PerformanceDataQueryResults(
            PerformanceDataQueryResults._Result("⅀♣◘ЪФЫВ>&<", "2", "3", null, null, null, null),
            PerformanceDataQueryResults._UsageTypes(
                listOf(
                    PerformanceDataQueryResults._Usage("4", "⅀♣◘"),
                    PerformanceDataQueryResults._Usage("6", "ЪФЫВ")
                )
            ),
            PerformanceDataQueryResults._XIComponents(listOf("рус1", "англ2")),
            PerformanceDataQueryResults._Periods(
                listOf(
                    PerformanceDataQueryResults._Period(
                        "HOURLY", listOf(
                            PerformanceDataQueryResults._Interval(
                                "2022-12-30T23:59:59.999+03:00",
                                "2022-12-31T23:59:59.999+03:00"
                            ),
                            PerformanceDataQueryResults._Interval("2022-12-30 23:59:59.9", "2022-12-31 23:59:59.9"),
                        )
                    )
                )
            ),
            PerformanceDataQueryResults._Data(
                PerformanceDataQueryResults._ColumnNames(listOf("INBOUND_CHANNEL", "OUTBOUND_CHANNEL")),
                PerformanceDataQueryResults._DataRows(
                    listOf(
                        PerformanceDataQueryResults._Row(
                            listOf(
                                PerformanceDataQueryResults._Entry(listOf("x")),
                                PerformanceDataQueryResults._Entry(listOf("y"))
                            )
                        )
                    )
                )
            )
        )
        val s: String = p.render()
        val q = PerformanceDataQueryResults.parse(s)
        show(q.Result.Code)
        require(q.Result.Code == "⅀♣◘ЪФЫВ>&<")
    }

    @Test
    fun performancemassload() {
        val d = Paths.get("C:\\data\\PerformanceDataQuery")
        if (d.exists()) {
            d.listDirectoryEntries("*.xml").forEach {
                val x = PlatformXmlReader(it.inputStream(), Charsets.UTF_8.toString())
                val p = PerformanceDataQueryResults.parse(x)    //it.readText(Charsets.UTF_8)
                show("$it\t${p.Result.Code}\t${p.perfdata().size}")
                p.perfdata().find { it.anomaly() }
            }
        }
    }

    @Test
    fun overviewmassload() {
        val d = Paths.get("C:\\data\\MessageOverviewQuery")
        if (d.exists()) {
            d.listDirectoryEntries("*.xml").forEach {
                val x = PlatformXmlReader(it.inputStream(), Charsets.UTF_8.toString())
                val q = MessageStatisticsQueryResults.parse(x)
                val p = MessageStatisticsQueryResults.parse(it.readText(Charsets.UTF_8))
                assert(p == q)
                show("$it\t${p.Result.Code}")
                if (p.Data != null) {
                    show("\t${p.Data!!.DataRows.value.size}")
                } else {
                    show("\t - no data")
                }
            }
        }
    }

    @Test
    fun influx_v1() {
        val d = Paths.get("C:\\data\\PerformanceDataQuery\\dayli_pph.xml").readText()
        val t = PerformanceDataQueryResults.parse(d)
        val a = t.Result.BeginTime!!.inst().toEpochSecond() * 1000    //загружать в ms
        val rez = StringBuilder()
        val lines = t.perfdata()
        val period = t.Result.PeriodType!!.lowercase()
        lines.filter { it.DELIVERY_SEMANTICS != "" }.forEach { x ->
            rez.append("""$period,QoS=${x.DELIVERY_SEMANTICS},INBOUND_CHANNEL=${x.INBOUND_CHANNEL},ACTION_NAME=${x.ACTION_NAME}""")
            rez.append(" message_counter=${x.MESSAGE_COUNTER}i,total_processing_time=${x.TOTAL_PROCESSING_TIME}i,avg_message_size=${x.AVG_MESSAGE_SIZE}i")
            rez.append(" ")
            rez.append(a)
            rez.append('\n')
        }
        Paths.get("C:\\data\\PerformanceDataQuery\\influx.dat").writeText(rez)
    }

    @Test
    fun stat_raw() {
        // без агрегации
        val csv = StringBuilder()
        csv.append("hour\tQOS\tqty\tAVG_MESSAGE_SIZE\tMIN_MESSAGE_SIZE\tMAX_MESSAGE_SIZE\n")
        Paths.get("C:\\data\\PerformanceDataQuery\\pph\\hourly").forEachDirectoryEntry("*.xml") { p ->
            val p3 = PerformanceDataQueryResults.parse(p.readText())
            val hour = p3.Result.BeginTime!!.inst().hour
            println(hour)
            p3.perfdata().filter { !it.anomaly() }.forEach { pr ->
                csv.append("$hour\t${pr.DELIVERY_SEMANTICS}\t${pr.MESSAGE_COUNTER}\t${pr.AVG_MESSAGE_SIZE}\t${pr.MIN_MESSAGE_SIZE}\t${pr.MAX_MESSAGE_SIZE}\n")
            }
        }
        Paths.get("c:\\data\\csv\\perf_raw.txt").writeText(csv)
    }

    @Test
    fun stat_aggr() {
        val raw = mutableListOf<PerformanceDataQueryResults>()
        Paths.get("C:\\data\\PerformanceDataQuery\\pph\\hourly").forEachDirectoryEntry("*.xml") { p ->
            raw.add(PerformanceDataQueryResults.parse(p.readText()))
        }
        // определяем размеры сообщений


        val strates = MPerfStat.makeHourly(raw)
        val js = MPerfStat.makeJS(strates, "титул")
        Paths.get("C:\\data\\PerformanceDataQuery\\report\\chart1.html").writeText(js)

        // выводим по порядку
        val csv = StringBuilder()
        csv.append("hour\tQOS\tСлой\tqty\tavg\n")
        strates.filter { it.qty > 0 }.forEach { pr ->
            csv.append("${pr.hour}\t${pr.qos}\t${pr.stratum}\t${pr.qty}\t${pr.avg}\n") //\t${pr.min}\t${pr.max}\n")
        }
        Paths.get("C:\\data\\PerformanceDataQuery\\report\\strates.txt").writeText(csv)

    }
}