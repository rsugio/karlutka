import KT.Companion.ris
import KT.Companion.x
import karlutka.parsers.pi.XiMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import java.io.ByteArrayOutputStream
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import javax.activation.DataHandler
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.*
import javax.mail.util.ByteArrayDataSource
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.outputStream
import kotlin.test.Test


class KXiMessageTest {
    val xmlmodule = SerializersModule {
    }

    val xmlserializer = XML(xmlmodule) {
        autoPolymorphic = true
    }

    fun postXI(contentType: String, path: Path) {
        val url = KT.urlS4
        val auth = KT.authS4

        val client: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .authenticator(auth)
            .build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofFile(path))
            .build()
        println(request.method() + " " + request.uri())
        println(request.headers())
        println("----------------------------------------------------")
        val response: HttpResponse<String> = client.send(request, BodyHandlers.ofString())
        println(response)
        println(response.headers())
        println(response.body())
    }

    @Test
    fun constructorS4() {
        val main = XiMessage.Main(
            3, 1, XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous,
            "00000000-0000-0000-0000-000000000033", null, "2023-04-14T11:31:01Z",
            XiMessage.PartyService(XiMessage.Party(), "TEST", XiMessage.Interface("", "")),
            null,
            XiMessage.Interface("urn:nlmk:po:I:ERP-SD:Reports:Report", "SI_RequestReport_InAsync")
        )
        val ximsg = XiMessage(XiMessage.Header(null, main, XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnce)))
        ximsg.setPayload("""<ns0:MT_RequestReport xmlns:ns0="urn:nlmk:po:I:ERP-SD:Reports:Report">
<SESSION_ID>00000000-0000-0000-0000-000000000021</SESSION_ID></ns0:MT_RequestReport>""".toByteArray(), "text/xml; charset=utf-8")
        val tmp = Paths.get("build/tmp.mime")
        ximsg.writeTo(tmp.outputStream())
        postXI(ximsg.getContentType(), tmp)
    }

    @Test
    fun constructorPO() {
        val main = XiMessage.Main(
            3, 1, XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous,
            "00000000-0000-0000-0000-000000000030", null, "2023-04-14T11:31:01Z",
            XiMessage.PartyService(XiMessage.Party(), "BC_TEST1"),
            XiMessage.PartyService(XiMessage.Party(), ""),
            XiMessage.Interface("urn:none", "SI_OutAsync")
        )
        val ximsg = XiMessage(XiMessage.Header(null, main, XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnce)))
        ximsg.setPayload("""<ns0:MT_RequestReport xmlns:ns0="urn:nlmk:po:I:ERP-SD:Reports:Report">
<SESSION_ID>00000000-0000-0000-0000-000000000021</SESSION_ID></ns0:MT_RequestReport>""".toByteArray(), "text/xml; charset=utf-8")
        val tmp = Paths.get("build/tmp.mime")
        ximsg.writeTo(tmp.outputStream())
        postXI(ximsg.getContentType(), tmp)
    }

    fun headers(contentId: String, contentType: String, contentDisposition: String): InternetHeaders {
        val ih = InternetHeaders()
        ih.addHeader("Content-ID", "<$contentId>")
        ih.addHeader("Content-Type", contentType)
        ih.addHeader("Content-Disposition", contentDisposition)
        return ih
    }

    @Test
    fun parse() {
        println(xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message1.xml")))
        println(xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message2.xml")))
        println(xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message3.xml")))
        println(xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/systemAck_PO75.xml")))
        println(xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/systemAck_S4.xml")))
    }

    @Test
    fun rfc2387_1() {
        val multipart: MimeMultipart = MimeMultipart()
        multipart.setSubType("related")

        val htmlPart = MimeBodyPart()
        htmlPart.contentID = "<SOAP>"
        htmlPart.setDescription("SOAP description")
        // messageBody contains html that references image
        // using something like <img src="cid:XXX"> where
        // "XXX" is an identifier that you make up to refer
        // to the image
        // messageBody contains html that references image
        // using something like <img src="cid:XXX"> where
        // "XXX" is an identifier that you make up to refer
        // to the image
        htmlPart.setText("messageBody", "utf-8", "html")
        multipart.addBodyPart(htmlPart)

        val imgPart = MimeBodyPart()
        imgPart.setDataHandler(DataHandler(ByteArrayDataSource("bytes", "text/xml")))
        imgPart.attachFile("c:\\temp\\1.txt")
        imgPart.contentID = "<PAYLOAD>"
        multipart.addBodyPart(imgPart)

        //message.setContent(multipart)
        println(multipart.contentType)
        multipart.writeTo(System.out)
    }

    @Test
    fun rfc_2() {
        val ih1 = InternetHeaders()
        ih1.addHeader("Content-ID", "<soap-8670dd5ad76611ed94c2000000417ca6@sap.com>")
        ih1.addHeader("Content-Disposition", "attachment;filename=\"soap-8670dd5ad76611ed94c2000000417ca6@sap.com.xml\"")
        ih1.addHeader("Content-Type", "text/xml; charset=utf-8")
        ih1.addHeader("Content-Description", "SOAP")
        val ba1 = "<SOAP/>".encodeToByteArray()
        val p1 = MimeBodyPart(ih1, ba1)

        val ih2 = InternetHeaders()
        ih2.addHeader("Content-Type", "multipart/related")
        ih2.addHeader("Content-ID", "<payload-866f7675d76611ed89de000000417ca6@sap.com>")
        ih2.addHeader("Content-Disposition", "attachment;filename=\"MainDocument.bin\"")
        ih2.addHeader("Content-Description", "MainDocument")
        val ba2 = "<MainDocument/>".encodeToByteArray()
        val p2 = MimeBodyPart(ih2, ba2)

        val mp: MimeMultipart = MimeMultipart("related")
        mp.addBodyPart(p1)
        mp.addBodyPart(p2)
        mp.writeTo(System.out)
    }

}

//    @Test
//    fun constructor() {
//        val messageid = "00000000-0000-0000-0000-000000000026"
//        val datetime = "2023-04-14T11:31:01Z"
//        val sender = XiMessage.PartyService(XiMessage.Party(), "BC_TEST1")
//        val receiver = XiMessage.PartyService(XiMessage.Party(), "")
//        val iface = XiMessage.Interface("urn:none", "SI_OutAsync")
//        val payload = "<РусскиеБуквыВперёдЪ> Х </РусскиеБуквыВперёдЪ>"
//        val dyn = XiMessage.DynamicConfiguration(
//            mutableListOf(
//                XiMessage.Record("urn:sapintegration", "FRIDAY_20230414", "РусскиеБуквы в DC"),
//            )
//        )
//        val _msid = messageid.replace("-", "")
//        val cidSoap = "soap-$_msid@sap.com"
//        val cidPayl = "payload-$_msid@sap.com"
//        val cidAttach1 = "attach1-$_msid@sap.com"
//        val cidAttach2 = "attach2-$_msid@sap.com"
//        val cidAttach3 = "attach3-$_msid@sap.com"
//
//        val main = XiMessage.Main(
//            3, 1, XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous, messageid, null, datetime,
//            sender, receiver, iface
//        )
//
//        val eo = XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnce)
//        //val eoio = XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnceInOrder, "_FRIDAY")
//
//        val payload1 = XiMessage.Payload("simple", "cid:$cidPayl", "Main20230414", XiMessage.PayloadType.Application)
//        val attachment1 = XiMessage.Payload("simple", "cid:$cidAttach1", "Attachment1", XiMessage.PayloadType.ApplicationAttachment)
//        val attachment2 = XiMessage.Payload("simple", "cid:$cidAttach2", "Attachment2", XiMessage.PayloadType.ApplicationAttachment)
//        val attachment3 = XiMessage.Payload("simple", "cid:$cidAttach3", "Attachment3", XiMessage.PayloadType.ApplicationAttachment)
//        val manifest = XiMessage.Manifest(mutableListOf(payload1, attachment1, attachment2, attachment3))
//        val envelope = XiMessage.Envelope(XiMessage.Header(main, eo, dyn), XiMessage.Body(manifest))
//        val smain = xmlserializer.encodeToString(envelope) + "\n"
//
//        val mp: MimeMultipart = MimeMultipart("related")
//        val ct = ContentType(mp.contentType)
//        val boundary = ct.getParameter("boundary")
//
//        val pSoap = MimeBodyPart(headers(cidSoap, "text/xml; charset=utf-8", "attachment; filename=\"$cidSoap.xml\""), smain.encodeToByteArray())
//        val pPayl = MimeBodyPart(headers(cidPayl, "application/xml; charset=utf-8", "attachment; filename=\"Payload.xml\""), payload.toByteArray())
//        val pAtt1 = MimeBodyPart(
//            headers(cidAttach1, "text/plain; charset=utf-8", "attachment; filename=\"Att1.xml\""),
//            "Русскiя  1111111111 аттача1".toByteArray()
//        )
//        val pAtt2 = MimeBodyPart(
//            headers(cidAttach2, "text/plain; charset=utf-8", "attachment; filename=\"Att2.xml\""),
//            "Русскiя 2 22222 аттача2".toByteArray()
//        )
//        val pAtt3 =
//            MimeBodyPart(headers(cidAttach3, "text/plain; charset=utf-8", "attachment; filename=\"Att3.xml\""), "Русскiя 3333 аттача3".toByteArray())
//        mp.addBodyPart(pSoap)
//        mp.addBodyPart(pPayl)
//        mp.addBodyPart(pAtt1)
//        mp.addBodyPart(pAtt2)
//        mp.addBodyPart(pAtt3)
//
//        val ct2 = "multipart/related; boundary=\"$boundary\""
//        println()
//
//        val baos = ByteArrayOutputStream(16384)
//        mp.writeTo(baos)
//        baos.close()
//
//        println(messageid)
//        println(ct2)
//
//        //println(String(baos.toByteArray()))
//
//        if (false) {
//            println("************************************ Kotlin -> PO 7.5")
//            var con = URL(KT.testPO).openConnection() as HttpURLConnection
//            con.addRequestProperty("Content-Type", ct2)
//            con.addRequestProperty("Authorization", KT.authPO)
//            con.doOutput = true
//            con.connect()
//            var os = con.getOutputStream()
//            os.write(baos.toByteArray())
//            os.close()
//            var rc = con.responseCode
//            println(rc)
//
//            var iss = if (rc < 300) con.inputStream else con.errorStream
//            var ba = iss.readAllBytes()
//            println(String(ba))
//            iss.close()
//        }
//        if (true) {
//            println("************************************ Kotlin -> CPI Neo")
//            var con = URL(KT.testCPI).openConnection() as HttpsURLConnection
//            println(ct2)
//            con.addRequestProperty("Content-Type", ct2)
//            con.addRequestProperty("Authorization", KT.authCPI)
//            con.doOutput = true
//            con.connect()
//            var os = con.getOutputStream()
//            os.write(baos.toByteArray())
//            os.close()
//            var rc = con.responseCode
//            println(rc)
//
//            var iss = if (rc < 300) con.inputStream else con.errorStream
//            var ba = iss.readAllBytes()
//            println(String(ba))
//            iss.close()
//        }
//        if (false) {
//            println("************************************ Kotlin -> S4")
//            var con = URL(KT.testS4).openConnection() as HttpURLConnection
//            println(ct2)
//            con.addRequestProperty("Content-Type", ct2)
//            con.addRequestProperty("Authorization", KT.authS4)
//            con.doOutput = true
//            con.connect()
//            var os = con.getOutputStream()
//            os.write(baos.toByteArray())
//            os.close()
//            var rc = con.responseCode
//            println(rc)
//
//            var iss = if (rc < 300) con.inputStream else con.errorStream
//            var ba = iss.readAllBytes()
//            println(String(ba))
//            iss.close()
//        }
//    }
