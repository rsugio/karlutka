package karlutka.models

import karlutka.parsers.pi.PerfMonitorServlet
import kotlinx.serialization.Serializable
import java.io.InputStreamReader

class MPerfStat {
    companion object {
        val captions = listOf("0-1К", "1-16К", "16-32К", "32-64К", "64-512К", "до 2М", "2М+")
        var sizes: List<Pair<Long, Long>> = listOf(
            0L to 1024L,
            1024L to 16384L,
            16384L to 32768L,
            32768L to 65536L,
            65536L to 524288L,
            524288L to 2097152L,
            2097152L to 9999999999L
        )

        fun makeHourly(inp: List<PerfMonitorServlet.PerformanceDataQueryResults>): List<MPerfStatHourly> {
            val strates = mutableListOf<MPerfStatHourly>()
            inp.forEach { p3 ->
                val hour = p3.Result.BeginTime!!.inst().hour
                // добавляем пустые слоты для красоты статистики
                sizes.forEachIndexed { x, _ ->
                    strates.add(MPerfStatHourly(hour, "EO", x))
                    strates.add(MPerfStatHourly(hour, "BE", x))
                }
                p3.perfdata().filter { !it.anomaly() }.forEach { pr ->
                    val stat = MPerfStatHourly.fromPerf(hour, pr)
                    val found =
                        strates.find { it.qos == stat.qos && it.hour == stat.hour && it.stratum == stat.stratum }
                    if (found == null)
                        strates.add(stat)
                    else
                        found.plus(stat)
                }
            }
            return strates
        }

        @Serializable
        class JS(val lst: List<String>)

        @Serializable
        class JT(val lst: List<JS>)

        fun toJT(t: List<List<String>>): String {
            return ""
        }

        fun makeJS(strates: List<MPerfStatHourly>, title: String = ""): String {
            val jsEO = StringBuilder()
            val jsBE = StringBuilder()
            jsEO.append("['Час','0-1К','1-16К','16-32К','32-64К','64-512К','до 2М','выше'],\n")
            jsBE.append("['Час','0-1К','1-16К','16-32К','32-64К','64-512К','до 2М','выше'],\n")
            var b = ""
            (0..23).forEach { h ->
                jsEO.append(b).append("['$h',")
                jsBE.append(b).append("['$h',")
                var c = ""
                sizes.forEachIndexed { x, _ ->
                    val eo = strates.find { it.hour == h && it.qos == "EO" && it.stratum == x }!!
                    val be = strates.find { it.hour == h && it.qos == "BE" && it.stratum == x }!!
                    jsEO.append(c).append("${eo.qty}")
                    jsBE.append(c).append("${be.qty}")
                    c = ","
                }
                jsEO.append("]").append("\n")
                jsBE.append("]").append("\n")
                b = ","
            }
            val text = Companion::class.java.getResourceAsStream("/performance/chart1.txt")
            requireNotNull(text)
            val s = InputStreamReader(text, Charsets.UTF_8).readText()
            return s.replace("\$title\$", title)
                .replace("\$BE\$", jsBE.toString())
                .replace("\$EO\$", jsEO.toString())
        }
    }

    class MPerfStatHourly(
        val hour: Int,
        val qos: String,
        val stratum: Int,
        var qty: Long = 0,
        var avg: Long = 0,
        var min: Long? = null,
        var max: Long? = null
    ) {

        fun plus(x: MPerfStatHourly) {
            val p = avg * qty
            val n = x.avg * x.qty
            qty += x.qty
            avg = (p + n) / qty
            if (min == null || min!! > x.min!!)
                min = x.min

            if (max == null || max!! < x.max!!)
                max = x.max
        }

        companion object {
            fun fromPerf(hour: Int, pr: PerfMonitorServlet.PerformanceTableRow): MPerfStatHourly {
                val qos = if (pr.DELIVERY_SEMANTICS == "EOIO")
                    "EO"
                else
                    pr.DELIVERY_SEMANTICS
                //TODO пока ищется по среднему, но надо учитывать также мин/макс
                val e = sizes.find { pr.AVG_MESSAGE_SIZE >= it.first && pr.AVG_MESSAGE_SIZE < it.second }
                requireNotNull(e)
                val stratum = sizes.indexOf(e)
                return MPerfStatHourly(
                    hour,
                    qos,
                    stratum,
                    pr.MESSAGE_COUNTER,
                    pr.AVG_MESSAGE_SIZE,
                    pr.MIN_MESSAGE_SIZE,
                    pr.MAX_MESSAGE_SIZE
                )
            }
        }
    }
}