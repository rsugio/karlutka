import KT.Companion.s
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
        val p3 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/03data.xml"))
        show(p3.perfdata().size)
        val p4 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/04datamin.xml"))
        show(p4.perfdata().size)
        val p5 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/05intervals.xml"))
        show(p5.Periods!!.value.size)
        val p6 = PerformanceDataQueryResults.parse(s("/pi_PerformanceDataQuery/06nodata.xml"))
        show(p6)
    }

    @Test
    fun composer() {
        val p = PerformanceDataQueryResults(
            PerformanceDataQueryResults._Result("⅀♣◘ЪФЫВ>&<", "2", "3", null, null, null, null),
            PerformanceDataQueryResults._UsageTypes(
                mutableListOf(
                    PerformanceDataQueryResults._Usage("4", "⅀♣◘"),
                    PerformanceDataQueryResults._Usage("6", "ЪФЫВ")
                )
            ),
            PerformanceDataQueryResults._XIComponents(mutableListOf("рус1", "англ2")),
            PerformanceDataQueryResults._Periods(
                mutableListOf(
                    PerformanceDataQueryResults._Period(
                        "HOURLY", mutableListOf(
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
                PerformanceDataQueryResults._ColumnNames(mutableListOf("INBOUND_CHANNEL", "OUTBOUND_CHANNEL")),
                PerformanceDataQueryResults._DataRows(
                    mutableListOf(
                        PerformanceDataQueryResults._Row(
                            mutableListOf(
                                PerformanceDataQueryResults._Entry(mutableListOf("x")),
                                PerformanceDataQueryResults._Entry(mutableListOf("y"))
                            )
                        )
                    )
                )
            )
        )
        val s: String = p.render()
        val q = PerformanceDataQueryResults.parse(s)
        require(p == q)
        show(p.Result.Code)
        require(p.Result.Code == "⅀♣◘ЪФЫВ>&<")
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
}