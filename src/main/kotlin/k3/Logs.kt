package k3

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class HttpAccessLog(
    val ip1: String,
    val ip2: String,
    val unk: String,    // //TODO -- прояснить
    val user: String,   // бывает минус, юзер или данные сертификата
    val dt: ZonedDateTime,
    val verb: String,   // ex. POST, GET, etc
    val resource: String,   // ex. /cxf/vendor/Commerce/StockTdLoad
    val proto: String,  // HTTP/1.1
    val rc: Int,        // код возврата 200, 401, 500
    val unk4: String,   // какое-то число или минус. Длина запроса, //TODO -- прояснить
    val int3: Int,      // какое-то число, длина ответа, //TODO -- прояснить
    val unk2: String,   // Http Host header, //TODO -- прояснить
    val unk3: String,   // минус или дичь, //TODO -- прояснить
) {
    companion object {
        private val rxLog =
            Regex("""(\d+\.\d+\.\d+\.\d+) \((\d+\.\d+\.\d+\.\d+)\) (\S+) ([^\[]+) \[(.+)\] (\S+) (.+) (HTTP/\d\.\d) (\d+) (.+) (\d+) (.+) (\S+)""")
        val dt = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH)

        fun toZoned(s:String): ZonedDateTime {
            // s == 11/Apr/2099:23:59:59 +0130
            return ZonedDateTime.parse(s, dt)
        }

        fun parseLine(line: String): HttpAccessLog? {
            val m = rxLog.matchEntire(line)
            if (m != null) {
                require(m.groupValues.size==14)
                var x = 1
                return HttpAccessLog(
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    toZoned(m.groupValues.get(x++)),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    Integer.valueOf(m.groupValues.get(x++)),
                    m.groupValues.get(x++),
                    Integer.valueOf(m.groupValues.get(x++)),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                )
            } else {
                return null
            }
        }

        fun parse(sc: Scanner): List<HttpAccessLog> {
            val lines = mutableListOf<HttpAccessLog>()
            var lo = 1
            while (sc.hasNextLine()) {
                val s = sc.nextLine()
                val log = parseLine(s)
                requireNotNull(log, {"Not parsed well line#$lo: $s"})
                lines.add(log)
                lo++
            }
            return lines
        }
    }
}
