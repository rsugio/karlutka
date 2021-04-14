package k3

import java.time.Instant
import java.util.*
import java.util.regex.Pattern

data class HttpAccessLog(
    val ip1: String,
    val ip2: String,
    val unk: String,    // minus all the time?
    val user: String,
    val dt: String,
    val verb: String,   // ex. POST, GET, etc
    val resource: String,   // ex. /cxf/vendor/Commerce/StockTdLoad
    val proto: String,  // HTTP/1.1
    val rc: Int,    // 200, 401, 500, etc
    val unk4: String,
    val int3: Int,
    val unk2: String,   // Http Host header ?
    val unk3: String,   // minus all the time?
) {
    companion object {
        private val rxIP =
            Regex("""(\d+\.\d+\.\d+\.\d+) \((\d+\.\d+\.\d+\.\d+)\) (\S+) ([^\[]+) \[(.+)\] (\S+) (.+) (HTTP/\d\.\d) (\d+) (.+) (\d+) (.+) (\S+)""")

        fun parseLine(line: String): HttpAccessLog? {
            val m = rxIP.matchEntire(line)
            if (m != null) {
                require(m.groupValues.size==14)
                var x = 1
                return HttpAccessLog(
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
                    m.groupValues.get(x++),
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
            while (sc.hasNextLine()) {
                val s = sc.nextLine()
                val l = parseLine(s)
                if (l==null) {
                    println(s)
                }
            }
            return lines
        }
    }
}
