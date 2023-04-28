import KT.Companion.s
import KT.Companion.x
import com.fasterxml.uuid.Generators
import karlutka.parsers.pi.XiMessage
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import java.net.Authenticator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.writeText
import kotlin.test.Test

class KXiMessageTest {
    val UUIDgenerator = Generators.timeBasedGenerator()

    val propCPI = KT.propAuth(Paths.get(".etc/cpi.properties"))
    val urlCpiBeOk = "${propCPI.get("url")}/cxf/rsug/xi1/be_ok"
    val urlCpiBeError = "${propCPI.get("url")}/cxf/rsug/xi1/be_err"
    val urlCpiEoOk = "${propCPI.get("url")}/cxf/rsug/xi1/eo_ok"
    val propABAP = KT.propAuth(Paths.get(".etc/abap.properties"))
    val propPO = KT.propAuth(Paths.get(".etc/po.properties"))
    val propPI = KT.propAuth(Paths.get(".etc/pi.properties"))

    val xiDC = XiMessage.DynamicConfiguration(mutableListOf(XiMessage.Record("urn:demo", "demo", "ДЕМО")))

    private val xmlmodule = SerializersModule {
    }
    private val xmlserializer = XML(xmlmodule) {
        autoPolymorphic = true
    }

    fun nextuuid() = UUIDgenerator.generate().toString()
    fun dateTimeSentNow() = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS))

    fun writePayload(name: String, content: String) {
        val payloads = Paths.get("build/payloads")
        if (!Files.isDirectory(payloads)) Files.createDirectories(payloads)
        payloads.resolve("$name.txt").writeText(content)
    }

    fun writePayload(name: String, xi: XiMessage) {
        val payloads = Paths.get("build/payloads")
        if (!Files.isDirectory(payloads)) Files.createDirectories(payloads)
        val os = payloads.resolve("$name.txt").outputStream()
        os.write("Content-Type: ${xi.getContentType()}\n-----------------------------------\n".toByteArray())
        xi.writeTo(os)
        os.close()
    }

    fun postXI(xin: XiMessage, url: String, auth: Authenticator): XiMessage? {
        val s = xin.header!!.Main!!.MessageId
        val client: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .authenticator(auth)
            .build()
        val tmpin = Paths.get("build/xi_$s.mime.txt")
        Files.deleteIfExists(tmpin)
        var tos = tmpin.outputStream()
        xin.writeTo(tos)
        tos.close()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", xin.getContentType())
            .POST(HttpRequest.BodyPublishers.ofFile(tmpin))
            .build()
        val response: HttpResponse<ByteArray> = client.send(request, BodyHandlers.ofByteArray())
        val rc = response.statusCode()
        val ct = response.headers().firstValue("conTent-typE")
        val tmpout = Paths.get("build/xiAnswer_$rc.mime.txt")
        Files.deleteIfExists(tmpout)
        tos = tmpout.outputStream()
        tos.write(response.body())
        tos.close()
        if (rc != 202 && ct.isPresent) {
            val xi = XiMessage(ct.get(), tmpout.readBytes())
            return xi
        }
        return null
    }


    @Test
    fun testCPI() {
        val main = XiMessage.Main(
            XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.synchronous,
            nextuuid(), null, dateTimeSentNow(),
            XiMessage.PartyService(XiMessage.Party(), "TEST", XiMessage.Interface("", "")),
            null,
            XiMessage.Interface("urn:test", "SI_Out")
        )
        val be = XiMessage.ReliableMessaging(XiMessage.QOS.BestEffort)
        val eo = XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnce, null, true, true, true, true)
        val ximsg = XiMessage(XiMessage.Header(main, be, xiDC))
        ximsg.setPayload("""<a>РусскиеБуквы</a>""".toByteArray(), "text/xml; charset=utf-8")
        ximsg.addAttachment("1русскиебуквы234ъ".toByteArray(), "text/plain; charset=utf-8")
        val ximsg2 = XiMessage(XiMessage.Header(main, eo, xiDC))
        ximsg2.setPayload("""<a>РусскиеБуквы2</a>""".toByteArray(), "text/xml; charset=utf-8")

        val xi2 = postXI(ximsg, urlCpiBeOk, propCPI.get("auth") as Authenticator)
        println(xi2)
        val xi3 = postXI(ximsg, urlCpiBeError, propCPI.get("auth") as Authenticator)
        println(xi3!!.fault!!.faultstring)
        val xi4 = postXI(ximsg2, urlCpiEoOk, propCPI.get("auth") as Authenticator)
        println(xi4)
    }

    @Test
    fun testPO() {
        val main = XiMessage.Main(
            XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous,
            nextuuid(), null, dateTimeSentNow(),
            XiMessage.PartyService(null, "BC_TEST1"),
            null,
            XiMessage.Interface("urn:none", "SI_OutAsync")
        )
        val eo = XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnce, null, false, false, false, false)
        val ximsg = XiMessage(XiMessage.Header(main, eo, xiDC))
        ximsg.setPayload("""<n:A xmlns:ns0="urn:demo">ЪЪЪЪЪЪЪЪЪЪЪЪЪЪЪЪЪЪЪ</n:A>""".toByteArray(), "text/xml; charset=utf-8")
        ximsg.addAttachment("1русскиебуквы234ъ".toByteArray(), "text/plain; charset=utf-8")
        val xi2 = postXI(ximsg, propPO.get("url") as String, propPO.get("auth") as Authenticator)
        println(xi2)
    }

    @Test
    fun testS4() {
        val iface = XiMessage.Interface("http://sap.com/xi/APPL/Global2", "PurchasingContractERPRequest_In_V1")
        val main = XiMessage.Main(
            XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.asynchronous,
            nextuuid(), null, dateTimeSentNow(),
            XiMessage.PartyService(null, "TEST", null),
            null,
            iface
        )
        val eo = XiMessage.ReliableMessaging(XiMessage.QOS.ExactlyOnceInOrder, "ASYNC", true, true, true, true)
        val ximsg = XiMessage(XiMessage.Header(main, eo, xiDC))
        ximsg.setPayload(s("/pi_XI/payloadABAP.xml").toByteArray(), "text/xml; charset=utf-8")
        ximsg.addAttachment("1русскиебуквы234ъ".toByteArray(), "text/plain; charset=utf-8")
        val xa = postXI(ximsg, propABAP.get("url") as String, propABAP.get("auth") as Authenticator)
        println("${xa!!.header!!.Ack!!.Status} ${xa.header!!.Ack!!.Category}")

        val iface2 = XiMessage.Interface("http://sap.com/xi/APPL/SE", "UnitOfMeasureByDimensionQueryResponse_In")
        val main2 = XiMessage.Main(
            XiMessage.MessageClass.ApplicationMessage, XiMessage.ProcessingMode.synchronous,
            nextuuid(), null, dateTimeSentNow(),
            XiMessage.PartyService(null, "TEST", null),
            null,
            iface2
        )
        val be = XiMessage.ReliableMessaging(XiMessage.QOS.BestEffort)
        val ximsg2 = XiMessage(XiMessage.Header(main2, be, xiDC))
        ximsg2.setPayload(s("/pi_XI/UnitOfMeasureByDimensionQuery_sync.xml").toByteArray(), "text/xml; charset=utf-8")
        ximsg2.addAttachment("1русскиебуквы234ъ".toByteArray(), "text/plain; charset=utf-8")
        val xa2 = postXI(ximsg2, propABAP.get("url") as String, propABAP.get("auth") as Authenticator)
        println(xa2!!.header?.Error?.Stack)
        xa2.manifest?.Payload?.forEach{
            println(it.getCid())
        }
    }

    @Test
    fun parsers() {
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message1.xml"))
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message2.xml"))
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message3.xml"))
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/message4.xml"))
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/systemAck_PO75.xml"))
        xmlserializer.decodeFromReader<XiMessage.Envelope>(x("/pi_XI/systemAck_S4.xml"))

        XiMessage("text/xml", s("/pi_XI/errorBE_CPI.xml").toByteArray())
        val m1 = XiMessage(s("/pi_XI/mime1_contentType.txt"), s("/pi_XI/mime1.txt").toByteArray())
        require(m1.header!!.ReliableMessaging!!.SystemAckRequested!!)
        XiMessage(s("/pi_XI/mime2_contentType.txt"), s("/pi_XI/mime2.txt").toByteArray())
        val m3 = XiMessage("text/xml;charset=UTF-8", s("/pi_XI/mime3.txt").toByteArray()).fault!!
        println("${m3.faultcode}||${m3.faultstring}")
        XiMessage("text/xml", s("/pi_XI/mime4.txt").toByteArray())
        val m5 = XiMessage("text/xml", s("/pi_XI/mime5.txt").toByteArray()).fault!!
        val s5 = m5.detail.error!!.Stack!!
        println(s5.substring(0, 200))
        XiMessage("text/xml", s("/pi_XI/mime6.txt").toByteArray())
        XiMessage("text/xml", s("/pi_XI/mime7.txt").toByteArray())
        XiMessage("text/xml", s("/pi_XI/mime8.txt").toByteArray())
        val ea = XiMessage("text/xml", s("/pi_XI/errorBE_ABAP.xml").toByteArray())
        println(ea.header!!.Error!!.AdditionalText)
    }

    @Test
    fun asyncAck() {
        // На вход приходит асинхронное сообщение, выдаём SystemAck с разными вариантами
        val src = XiMessage(s("/pi_XI/mime2_contentType.txt"), s("/pi_XI/mime2.txt").toByteArray())
        require(src.header!!.ReliableMessaging!!.QualityOfService.isAsync())
        val ok1 = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.OK)
        writePayload("asyncAckOk1", ok1.encodeToString())
        writePayload("asyncAckOk1_mime", XiMessage(ok1))

        val ok2 = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.OK, XiMessage.AckCategory.permanent)
        writePayload("asyncAckOk2", ok2.encodeToString())
        val ok3 = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.OK, XiMessage.AckCategory.transient)
        writePayload("asyncAckOk3", ok3.encodeToString())
        val error1 = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.Error, XiMessage.AckCategory.permanent)
        writePayload("asyncAckError1", error1.encodeToString())
        val error2 = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.Error, XiMessage.AckCategory.transient)
        writePayload("asyncAckError2", error2.encodeToString())
        val notSupported = src.systemAck(nextuuid(), dateTimeSentNow(), XiMessage.AckStatus.AckRequestNotSupported)
        writePayload("asyncAckNotSupported", notSupported.encodeToString())
    }

    @Test
    fun syncAnswers() {
        // На вход приходит синхронное сообщение, выдаём разные варианты ответов
        val src = XiMessage(s("/pi_XI/mime1_contentType.txt"), s("/pi_XI/mime1.txt").toByteArray())
        require(!src.header!!.ReliableMessaging!!.QualityOfService.isAsync())
        val e1 = XiMessage.Error(XiMessage.ErrorCategory.XIServer, XiMessage.ErrorCode("INTERNAL", "VALUE"), null, null, null, null, "Add", "Stack")
        val f1 = src.fault("soap:Server", "123", "http://sap.com/xi/XI/Message/30", e1)
        writePayload("syncFault1", f1.encodeToString())
        // сюда можно добавить остальные варианты фолтов

        val an1 = src.syncResponse(nextuuid(), dateTimeSentNow())
        an1.setPayload("<Результат/>".toByteArray(), "text/xml; charset=UTF-8")
        an1.addAttachment("ЗАЗАЗАЗАЗАЗАЗАЗА".toByteArray(), "text/plain; charset=UTF-8")
        writePayload("syncAnswer1", an1)

        val e2 = src.syncError(nextuuid(), dateTimeSentNow(), e1)
        writePayload("syncError1", e2)
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

    @Test
    fun guid() {
        val v = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS))
        println(v)  //"2023-04-27T14:45:08.918Z"
        println(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))

        val s = "4f4d98db-e4be-11ed-c15d-0000004c2912"
        println(UUID.fromString(s).version())       // для проверок чужих гуидов на версию
        val gen = Generators.timeBasedGenerator()
        repeat(2, {
            val uuid = gen.generate()
            println("$uuid: ${uuid.version()}")
        })
    }
}
