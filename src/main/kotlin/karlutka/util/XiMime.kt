package karlutka.util

import java.io.InputStream
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Миме-сообщение для XI протокола
 */
class XiMime {
    constructor(ba: ByteArray) {
        //
        val ds = ByteArrayDataSource(ba, "multipart/related")
        val mm = MimeMultipart(ds)
        // закладываемся на то что 0й всегда иксай
        var xi: BodyPart? = null
        try {
            xi = mm.getBodyPart(0)
        } catch (_: MessagingException) {
        }
        if (xi != null) {
            xiContentId = xi.getHeader("cONTENT-iD").toString() // регистронезависимо должно быть
            xiBody = String(xi.inputStream.readAllBytes(), Charsets.UTF_8)

            // закладываемся что 1й (если есть) это пейлоад
            if (mm.count == 1) {
                payloadContentId = null
                payloadContentType = null
                payload = ByteArray(0)
            } else {
                val bp = mm.getBodyPart(1)
                payloadContentId = bp.getHeader("CoNTeNt-id").toString()
                payloadContentType = bp.getHeader("CoNTeNt-tYPE").toString()
                when (bp.content) {
                    is InputStream -> {
                        payload = (bp.content as InputStream).readBytes()
                    }

                    is String -> {
                        //TODO добавить тест и обработку на случай другой кодировки. Нужно сперва отправить иксай-сообщение вручную
                        payload = (bp.content as String).toByteArray(Charsets.UTF_8)
                    }

                    is ByteArray -> payload = bp.content as ByteArray
                    else -> TODO("надо разобрать этот случай: ${bp.content.javaClass}")
                }
            }
            var i = 2
            while (i < mm.count) {
                val bx = mm.getBodyPart(i)
                parts.add(bx)
                i++
            }
        } else {
            val bp = MimeBodyPart(ba.inputStream())
            xiBody = String(bp.inputStream.readAllBytes(), Charsets.UTF_8)
            xiContentId = bp.getHeader("cONTENT-iD").toString() // регистронезависимо должно быть
            payloadContentId = null
            payloadContentType = null
            payload = ByteArray(0)
        }
    }

    val xiBody: String
    val xiContentId: String         // <soap-005056BF8D881EED84E458229A6789C2@sap.com>
    val payloadContentId: String?   // <payload-005056BF8D881EED84E458229A6789C2@sap.com>
    val payloadContentType: String?
    val payload: ByteArray
    val parts: MutableList<BodyPart> = mutableListOf()    // все части кроме xi-заголовка и пейлоада
}