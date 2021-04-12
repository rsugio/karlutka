package k1

import java.util.*
import java.util.regex.Pattern

// это не сложный квери
class NotSoComplexQuery(s: Scanner) {
    val headers: MutableList<String> = mutableListOf()
    val lines: MutableList<MutableMap<String, String>> = mutableListOf()

    init {
        parse(s)
    }

    private fun parse(s: Scanner) {
        val xp = Regex("<(td|th)>\\s*(.+?)\\s*</\\1>", RegexOption.DOT_MATCHES_ALL)
        val ahref = Regex("<a.+href=\"(.+)\">")

        s.findWithinHorizon(Pattern.compile("""<input type=submit name=action value="Start query" />.+<table border=1>""",
            Pattern.DOTALL), Int.MAX_VALUE)
        val w = s.findAll(Pattern.compile("<tr>\\s*(.+?)\\s*</tr>", Pattern.DOTALL))
        w.forEach {
            val tr = it.group(1)
            val tdth = xp.findAll(tr)
            val mp: MutableMap<String, String> = mutableMapOf()
            tdth.forEachIndexed { i, m2 ->
                when {
                    m2.groupValues[1] == "th" -> headers.add(m2.groupValues[2])
                    m2.groupValues[1] == "td" -> {
                        var v = m2.groupValues[2]
                        if (headers[i] in arrayOf("Raw", "WSDL") && !arrayOf("", "&nbsp;").contains(v)) {
                            v = ahref.find(v)?.groupValues?.get(1) as String
                        }
                        mp[headers[i]] = v
                    }
                    else -> error("Unexpected tag ${m2.groupValues[1]}")
                }
            }
            if (!mp.isEmpty()) lines.add(mp)
        }
    }

    companion object {
        val allowedRep = arrayOf("XI_TRAFO", "ifmmessif")
        fun getContentType() = "application/x-www-form-urlencoded"

        // УРЛ не очень-то важен, там один HMI сервак отвечает а меняется лишь шапка и форма
        fun getUrlRep(pihost: String) = "$pihost/rep/support/SimpleQuery"
        fun getUrlDir(pihost: String) = "$pihost/dir/support/SimpleQuery"

        fun repQuery(types: String): String {
            requireNotNull(allowedRep.contains(types))
            return "qc=All+software+components&swcL=Local+Software+Components&underL=true&changeL=true&syncTabL=true&deletedL=N&xmlReleaseL=7.1&queryRequestXMLL=&types=$types&qcActiveL0=true&qcValueL0=" +
                    "&result=RA_XILINK" +
                    "&result=TEXT" +
                    "&result=NAME" +
                    "&result=NAMESPACE" +
                    "&result=OBJECTID" +
                    "&result=VERSIONID" +
                    "&action=Start+query"
        }

//        fun dirQuery(types: String = ""): String {
//            requireNotNull(allowedDir.contains(types))
//            return "qc=All+software+components&swcL=Local+Software+Components&underL=true&changeL=true&syncTabL=true&deletedL=N&xmlReleaseL=7.1&queryRequestXMLL=&types=$types&qcActiveL0=true&qcValueL0=&result=RA_XILINK&result=NAME&result=NAMESPACE&result=OBJECTID&result=VERSIONID&action=Start+query"
//        }
    }
}