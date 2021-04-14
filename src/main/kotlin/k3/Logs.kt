package k3

import java.io.InputStream
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
            return """ip1=$ip1
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

/**
 * Разбор ljs_trace_\d+
 */
enum class LjsTraceLevelEnum { ERROR }
class LjsTraceLine(
    val dt: ZonedDateTime,
    val level: LjsTraceLevelEnum,
    val component: String,
    val unk1: String,   // видел только пусто
    val user: String,
    val threadName: String,
    val unk2: String,   // видел только пусто
    val unk3: String,   // видел только пусто
    val nodeId: String, // j1f3f5ee1
    val unk4: String,   // [na, 1060B3D4396247CBA5379F638CC644CB, и другие гуиды]
    val unk5: String,  // аналогично unk4
    val unk6: String,  // аналогично unk4
    val unk7: String,  // [na, 0, 1]
    val msg: String,   // какое-то сообщение
    // далее |\n    0x7C 0x0A
) {
    companion object {
        val dt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss'#'x'#'", Locale.ENGLISH)

        fun toZoned(s: String): ZonedDateTime {
            // s == 2021 04 14 00:00:07#+00#
            return ZonedDateTime.parse(s, dt)
        }

        @ExperimentalStdlibApi
        fun parse(sc: InputStream): List<LjsTraceLine> {
            val lines = mutableListOf<LjsTraceLine>()
            var todo = true

            fun readHash(): String {
                val ba = StringBuilder()
                var i = sc.read()
                while (todo) {
                    when (i) {
                        -1 -> {
                            todo = false
                            return ba.toString()
                        }
                        0x23 -> {   //#==0x23
                            return ba.toString()
                        }
                        else -> {
                            val s = Char(i)
                            ba.append(s)
                        }
                    }
                    i = sc.read()
                }
                return ""
            }

            fun readLn(): String {
                val ba = StringBuilder()
                var i = sc.read()
                while (todo) {
                    if (i == -1) {
                        todo = false
                        return ba.toString()
                    } else if (i == 0x7C) {
                        i = sc.read()
                        if (i == 0x0A || i == -1) {
                            return ba.toString()
                        } else {
                            ba.append(Char(i))
                        }
                    } else {
                        ba.append(Char(i))
                    }
                    i = sc.read()
                }
                return ""
            }
            while (todo) {
                val s = sc.readNBytes(24)
                if (s.isEmpty()) break
                val zdt = toZoned(String(s))
                val level = readHash()
                val comp = readHash()
                val unk1 = readHash()
                val user = readHash()
                val thread = readHash()
                val unk2 = readHash()
                val unk3 = readHash()
                val nodeId = readHash()
                val unk4 = readHash()
                val unk5 = readHash()
                val unk6 = readHash()
                val unk7 = readHash()
                val msg = readLn()
                val ln = LjsTraceLine(zdt,
                    LjsTraceLevelEnum.valueOf(level), comp, unk1, user, thread,
                    unk2, unk3, nodeId, unk4, unk5, unk6, unk7, msg)
                lines.add(ln)
            }
            return lines
        }

        fun distinct(lst: List<LjsTraceLine>): String {
            val level = lst.map { it.level }.distinct()
            val component = lst.map { it.component }.distinct()
            val user = lst.map { it.user }.distinct()
            val threadName = lst.map { it.threadName }.distinct()

            val unk1 = lst.map {it.unk1}.distinct()
            val unk2 = lst.map {it.unk2}.distinct()
            val unk3 = lst.map {it.unk3}.distinct()
            val unk4 = lst.map {it.unk4}.distinct()
            val unk5 = lst.map {it.unk5}.distinct()
            val unk6 = lst.map {it.unk6}.distinct()
            val unk7 = lst.map {it.unk7}.distinct()
            val nodeId = lst.map {it.nodeId}.distinct()
            return """level=$level
user=$user
nodeId=$nodeId
threadName=$threadName
component=$component
unk1 = $unk1
unk7 = $unk7
"""
        }
    }
}
