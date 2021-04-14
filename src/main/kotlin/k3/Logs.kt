package k3

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Пакет k3, то есть относится к CPI.
 * HttpAccessLog -- разбор логов посещений
 */
@Suppress("unused")
class HttpAccessLogLine(
    val ip1: String,
    val ip2: String,
    val unk: String,    // виден только минус. ХЗ!
    val user: String,   // [-, S00000000, P00000000, e450099-iflmap.hcisbp.ru1.hana.ondemand.com@DigiCert Global CA G2, oauth_client_ed314486-ffff-ffff-ac87-c1471a23c895]
    val dt: ZonedDateTime,
    val verb: String,   // пока видны только POST и GET. Наверное могут быть любые.
    val resource: String,   // ex. /cxf/vendor/Commerce/StockTdLoad
    val proto: String,  // HTTP/1.1
    val rc: Int,        // код возврата 200, 401, 500
    val len: Int?,      // какое-то число или минус. Длина запроса?
    val int3: Int,      // какое-то число, длина ответа? время?
    val unk2: String,   // Http Host header? в целом неинформативно
    val unk3: String,   // минус или для сертификатов/oauth какая-то инфа вида [6aef72b364eb43db8ae251f21fc338db/0A3EC03039436073CAFC000500000000/1/6AEF72B364EB43DB8AE251F21FC338DB]
) {
    companion object {
        private val rxLog =
            Regex("""(\d+\.\d+\.\d+\.\d+) \((\d+\.\d+\.\d+\.\d+)\) (\S+) ([^\[]+) \[(.+)] (\S+) (.+) (HTTP/\d\.\d) (\d+) (.+) (\d+) (.+) (\S+)""")
        val dt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH)

        fun toZoned(s: String): ZonedDateTime {
            // s == 11/Apr/2099:23:59:59 +0130
            return ZonedDateTime.parse(s, dt)
        }

        private fun toIntOrNull(s: String): Int? =
            if (s == "-") null else Integer.valueOf(s)

        fun parseLine(line: String): HttpAccessLogLine? {
            val m = rxLog.matchEntire(line)
            if (m != null) {
                require(m.groupValues.size == 14)
                var x = 1
                @Suppress("UNUSED_CHANGED_VALUE")
                return HttpAccessLogLine(
                    m.groupValues[x++],
                    m.groupValues[x++],
                    m.groupValues[x++],
                    m.groupValues[x++],
                    toZoned(m.groupValues[x++]),
                    m.groupValues[x++],
                    m.groupValues[x++],
                    m.groupValues[x++],
                    Integer.valueOf(m.groupValues[x++]),
                    toIntOrNull(m.groupValues[x++]),
                    Integer.valueOf(m.groupValues[x++]),
                    m.groupValues[x++],
                    m.groupValues[x]
                )
            } else {
                return null
            }
        }

        fun parse(sc: Scanner): List<HttpAccessLogLine> {
            val lines = mutableListOf<HttpAccessLogLine>()
            var lo = 1
            while (sc.hasNextLine()) {
                val s = sc.nextLine()
                val log = parseLine(s)
                requireNotNull(log, { "Not parsed well line#$lo: $s" })
                lines.add(log)
                lo++
            }
            return lines
        }

        /**
         * Это просто вспомогательная смотрелка глазами что же приходит
         */
        fun distinct(lst: List<HttpAccessLogLine>): String {
            val ip1 = lst.map { it.ip1 }.distinct()
            val ip2 = lst.map { it.ip2 }.distinct()
            val unk = lst.map { it.unk }.distinct()
            val user = lst.map { it.user }.distinct()
            val resource = lst.map { it.resource }.distinct()
            val verb = lst.map { it.verb }.distinct()
            val proto = lst.map { it.proto }.distinct()
            val rc = lst.map { it.rc }.distinct()
//            val unk4 = lst.map { it.len }.distinct()
//            val int3 = lst.map { it.int3 }.distinct()
            val unk2 = lst.map { it.unk2 }.distinct()
            val unk3 = lst.map { it.unk3 }.distinct()
            return """
ip1=$ip1
ip2=$ip2
unk=$unk
user=$user
resource=${resource.subList(0, minOf(10, resource.size))}
verb=$verb
proto=$proto
rc=$rc
unk2=$unk2
unk3=${unk3.subList(0, minOf(10, resource.size))}"""
        }
    }
}
