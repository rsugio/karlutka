package karlutka.parsers.pi

import karlutka.serialization.KSoap.Companion.xmlserializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.mail.internet.ContentType
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

const val xmlnsSOAP: String = "http://schemas.xmlsoap.org/soap/envelope/"
const val xmlnsXI30: String = "http://sap.com/xi/XI/Message/30"
const val xmlnsXI30prefix: String = "SAP"

class XiMessage {
    enum class PayloadType { Application, ApplicationAttachment }
    enum class QOS { ExactlyOnce, ExactlyOnceInOrder, BestEffort }
    enum class MessageClass { ApplicationMessage, ApplicationResponse, SystemAck }
    enum class ProcessingMode { asynchronous, synchronous }

    val mp: MimeMultipart
    val ct: ContentType
    val boundary: String
    val payloadHrefs = mutableMapOf<String, Payload>()
    val payloadBodies = mutableMapOf<String, MimeBodyPart>()

    val header: Header?                 // нет для Fault
    val fault: Fault?
    private val _msid: String?          // используется только для вывода в случае непустого Header (нет вывода при Fault)
    private var writeFinished: Boolean = false

    constructor(h: Header) {
        this.mp = MimeMultipart("related")
        this.ct = ContentType(mp.contentType)
        this.boundary = ct.getParameter("boundary")

        this.header = h
        this.fault = null
        // Если требуется выдать ответ с ошибкой как из абапа, то _msid можно делать пустым и не выдавать multipart/related
        this._msid = h.Main!!.MessageId.replace("-", "")
    }

    constructor(f: Fault) {
        this._msid = null
        this.mp = MimeMultipart("related")
        this.ct = ContentType(mp.contentType)
        this.boundary = "" // ct.getParameter("boundary")

        this.header = null
        this.fault = f
    }

    constructor(contentType: String, ba: ByteArray) {
        this._msid = null
        ct = ContentType(contentType)
        val e: Envelope
        if (ct.match("multipart/*")) {
            val ds = ByteArrayDataSource(ba, contentType)
            this.mp = MimeMultipart(ds)
            this.boundary = ct.getParameter("boundary")
            val soap = this.mp.getBodyPart(this.ct.getParameter("start"))
            e = xmlserializer.decodeFromString<Envelope>(soap.content as String)
        } else {
            mp = MimeMultipart()
            this.boundary = ""
            val x = PlatformXmlReader(ba.inputStream(), ct.getParameter("charset") ?: "UTF-8")
            e = xmlserializer.decodeFromReader<Envelope>(x)
        }
        this.header = e.Header
        if (this.header == null) {
            this.fault = e.Body.Fault
            // Ошибка
        } else {
            this.fault = null
            // Манифеста нет для успешного ответа если это Ack
            e.Body.Manifest?.Payload?.forEach { p ->
                val part = mp.getBodyPart(p.getCid())
                requireNotNull(part, { p.getCid() })
                val cnt = part.inputStream.readAllBytes()
                val scnt = String(cnt, StandardCharsets.UTF_8)              //TODO переделать на чтение из заголовка
                println("${p.getCid()} contains ${cnt.size} bytes: $scnt")
            }
        }
    }

    fun setPayload(body: ByteArray, contentType: String, description: String? = null) {
        require(!writeFinished)
        requireNotNull(this.header, { "Установить пейлоад можно только для непустого заголовка" })
        require(this.fault == null, { "Установить пейлоад нельзя для ошибки" })
        val _cidPayload = "payload-$_msid@rsug.ru"
        val payload = Payload("simple", "cid:$_cidPayload", "Payload", PayloadType.Application, description)
        payloadHrefs.put(_cidPayload, payload)

        val ih = InternetHeaders()
        ih.addHeader("Content-ID", "<$_cidPayload>")
        ih.addHeader("Content-Type", contentType)
        ih.addHeader("Content-Disposition", "attachment; filename=\"Payload.xml\"")
        payloadBodies.put(_cidPayload, MimeBodyPart(ih, body))
    }

    fun addAttachment(body: ByteArray, contentType: String, description: String? = null) {
        require(!writeFinished)
        val _cid = "attach-$_msid@rsug.ru"
        val payload = Payload("simple", "cid:$_cid", "Attach", PayloadType.ApplicationAttachment, description)
        payloadHrefs.put(_cid, payload)

        val ih = InternetHeaders()
        ih.addHeader("Content-ID", "<$_cid>")
        ih.addHeader("Content-Type", contentType)
        ih.addHeader("Content-Disposition", "attachment; filename=\"Attach.xml\"")
        payloadBodies.put(_cid, MimeBodyPart(ih, body))
    }

    fun writeTo(o: OutputStream) {
        //TODO Нужна развилка для Fault и нет
        if (!writeFinished) {
            val manifest = Manifest(payloadHrefs.values.toMutableList())
            val envelope = Envelope(header, Body(manifest))
            val smain = xmlserializer.encodeToString(envelope)

            val ih = InternetHeaders()
            ih.addHeader("Content-ID", "<soap-$_msid@rsug.ru>")
            ih.addHeader("Content-Type", "text/xml; charset=utf-8")
            ih.addHeader("Content-Disposition", "attachment; filename=\"soap-$_msid.xml\"")
            val mbpSAP = MimeBodyPart(ih, smain.toByteArray())
            mp.addBodyPart(mbpSAP)

            payloadBodies.forEach { (k, mbp) ->
                mp.addBodyPart(mbp)
            }
            writeFinished = true
        }
        mp.writeTo(o)
    }

    fun getContentType(): String {
        val boundary = ct.getParameter("boundary")
        return "multipart/related; boundary=\"$boundary\""
    }

    @Serializable
    @XmlSerialName("Envelope", xmlnsSOAP, "SOAP")
    class Envelope(
        @XmlElement(true)
        val Header: Header? = null,             // нет для Fault
        @XmlElement(true)
        val Body: Body,
    )

    @Serializable
    @XmlSerialName("Body", xmlnsSOAP, "SOAP")
    class Body(
        @XmlElement(true)
        val Manifest: Manifest? = null,          // нет для Ack и Fault
        @XmlElement(true)
        val Fault: Fault? = null
    )

    @Serializable
    @XmlSerialName("Fault", xmlnsSOAP, "SOAP")
    class Fault(
        @XmlElement(true)
        @XmlSerialName("faultcode", "", "")
        val faultcode: String,
        @XmlElement(true)
        @XmlSerialName("faultstring", "", "")
        val faultstring: String,
        @XmlElement(true)
        @XmlSerialName("faultactor", "", "")
        val faultactor: String? = null,
        @XmlElement(true)
        @XmlSerialName("detail", "", "")
        val detail: Detail
    )

    @Serializable
    @XmlSerialName("Header", xmlnsSOAP, "SOAP")
    class Header(
        @XmlElement(true)
        val Main: Main? = null,                                     // нет для ошибок в абапе
        @XmlElement(true)
        val ReliableMessaging: ReliableMessaging? = null,           // нет для Ack
        @XmlElement(true)
        var DynamicConfiguration: DynamicConfiguration? = null,
        @XmlElement(true)
        val Ack: Ack? = null,
        @XmlElement(true)
        var System: System? = null,
        @XmlElement(true)
        var Diagnostic: Diagnostic? = null,
        @XmlElement(true)
        var HopList: HopList? = null,
        @XmlElement(true)
        var Passport: Passport? = null,
        @XmlElement(true)
        @XmlSerialName("RunTime", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        var RunTime: CompactFragment? = null,
        @XmlElement(true)
        @XmlSerialName("PerformanceHeader", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        var PerformanceHeader: CompactFragment? = null,
        @XmlElement(true)
        @XmlSerialName("Trace", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        var Trace: CompactFragment? = null,
        @XmlElement(true)
        var Error: Error? = null,
    )

    @Serializable
    class Detail(
        @XmlElement(true)
        val error: Error? = null,
    )

    @Serializable
    @XmlSerialName("Error", xmlnsXI30, xmlnsXI30prefix)
    class Error(
        @XmlElement(true)
        val Category: String,
        @XmlElement(true)
        val Code: String,
        @XmlElement(true)
        val P1: String? = null,
        @XmlElement(true)
        val P2: String? = null,
        @XmlElement(true)
        val P3: String? = null,
        @XmlElement(true)
        val P4: String? = null,
        @XmlElement(true)
        val AdditionalText: String? = null,
        @XmlElement(true)
        val Stack: String? = null,

        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )


    @Serializable
    @XmlSerialName("Ack", xmlnsXI30, xmlnsXI30prefix)
    class Ack(
        @XmlElement(true)
        val Status: String,                     // OK
        @XmlElement(true)
        val Category: String? = null,           // permanent for ABAP, null for Java
        val arrivedAtFinalReceiver: Boolean? = null,    // заполняется в абапе при запросе Ack
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("Main", xmlnsXI30, xmlnsXI30prefix)
    class Main(
        @XmlElement(true)
        @XmlSerialName("MessageClass", xmlnsXI30, xmlnsXI30prefix)
        val MessageClass: MessageClass, // = "ApplicationMessage",
        @XmlElement(true)
        @XmlSerialName("ProcessingMode", xmlnsXI30, xmlnsXI30prefix)
        val ProcessingMode: ProcessingMode, // = "asynchronous",
        @XmlElement(true)
        val MessageId: String,
        @XmlElement(true)
        val RefToMessageId: String? = null,
        @XmlElement(true)
        val TimeSent: String,
        @XmlElement(true)
        @XmlSerialName("Sender", xmlnsXI30, xmlnsXI30prefix)
        val Sender: PartyService,
        @XmlElement(true)
        @XmlSerialName("Receiver", xmlnsXI30, xmlnsXI30prefix)
        val Receiver: PartyService? = null,
        @XmlElement(true)
        val Interface: Interface? = null,
        // атрибуты
        val versionMajor: Int = 3,
        val versionMinor: Int = 1,
        // атрибуты по умолчанию - в конце
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
    )

    @Serializable
    class PartyService(
        @XmlElement(true)
        val Party: Party? = null,
        @XmlElement(true)
        val Service: String? = null,
        @XmlElement(true)
        val Interface: Interface? = null
    )

    @Serializable
    @XmlSerialName("Party", xmlnsXI30, xmlnsXI30prefix)
    class Party(
        @XmlValue(true)
        val content: String? = null,
        val agency: String = "http://sap.com/xi/XI",
        val scheme: String = "XIParty",
    )

    @Serializable
    @XmlSerialName("Interface", xmlnsXI30, xmlnsXI30prefix)
    class Interface(
        val namespace: String,
        @XmlValue(true)
        val content: String = ""
    )

    @Serializable
    @XmlSerialName("ReliableMessaging", xmlnsXI30, xmlnsXI30prefix)
    class ReliableMessaging(
        @XmlElement(true)
        @XmlSerialName("QualityOfService", xmlnsXI30, xmlnsXI30prefix)
        val QualityOfService: QOS,
        @XmlElement(true)
        val QueueId: String? = null,

        val ApplicationAckRequested: Boolean? = null,
        val ApplicationErrorAckRequested: Boolean? = null,
        val SystemAckRequested: Boolean? = null,
        val SystemErrorAckRequested: Boolean? = null,

        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1
    )

    @Serializable
    @XmlSerialName("System", xmlnsXI30, xmlnsXI30prefix)
    class System(
        @XmlElement(true)
        @XmlSerialName("Record", xmlnsXI30, xmlnsXI30prefix)
        val Record: MutableList<Record> = mutableListOf(),
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("Record", xmlnsXI30, xmlnsXI30prefix)
    class Record(
        val namespace: String,
        val name: String,
        @XmlValue(true)
        val content: String? = null
    )

    @Serializable
    @XmlSerialName("Diagnostic", xmlnsXI30, xmlnsXI30prefix)
    class Diagnostic(
        @XmlElement(true)
        val TraceLevel: String,
        @XmlElement(true)
        val Logging: String,
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("HopList", xmlnsXI30, xmlnsXI30prefix)
    class HopList(
        @XmlElement(true)
        @XmlSerialName("Hop", xmlnsXI30, xmlnsXI30prefix)
        val HopList: MutableList<Hop> = mutableListOf(),
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    class Hop(
        val timeStamp: String,
        val wasRead: Boolean,
        @XmlElement(true)
        val Engine: Engine,
        @XmlElement(true)
        val Adapter: Adapter,
        @XmlElement(true)
        val MessageId: String,
        @XmlElement(true)
        val Info: String? = null,
    )

    @Serializable
    @XmlSerialName("Engine", xmlnsXI30, xmlnsXI30prefix)
    class Engine(
        val type: String,           //BS, IS
        @XmlValue(true)
        val content: String?
    )

    @Serializable
    @XmlSerialName("Adapter", xmlnsXI30, xmlnsXI30prefix)
    class Adapter(
        val namespace: String = "http://sap.com/xi/XI/System",
        @XmlValue(true)
        val content: String = "XI"
    )

    @Serializable
    @XmlSerialName("Passport", xmlnsXI30, xmlnsXI30prefix)
    class Passport(
        @XmlElement(true)
        val PassportHash: String,
        @XmlElement(true)
        val Transaction_ID: String
    )

    @Serializable
    @XmlSerialName("DynamicConfiguration", xmlnsXI30, xmlnsXI30prefix)
    class DynamicConfiguration(
        @XmlElement(true)
        val Record: MutableList<Record> = mutableListOf(),
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("Manifest", xmlnsXI30, xmlnsXI30prefix)
    class Manifest(
        @XmlElement(true)
        @XmlSerialName("Payload", xmlnsXI30, xmlnsXI30prefix)
        val Payload: MutableList<Payload> = mutableListOf(),
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
    )

    @Serializable
    class Payload(
        @XmlSerialName("type", "http://www.w3.org/1999/xlink", "xlink")
        val xlinktype: String? = null, // simple. Пусто в абапе
        @XmlSerialName("href", "http://www.w3.org/1999/xlink", "xlink")
        val href: String, // cid:payload-005056BF38E01EDDB58C9B5814A60450@sap.com
        @XmlElement(true)
        val Name: String,           //
        @XmlElement(true)
        @XmlSerialName("Type", xmlnsXI30, xmlnsXI30prefix)
        val Type: PayloadType,
        @XmlElement(true)
        val Description: String? = null,
    ) {
        fun getCid(): String {
            require(href.lowercase().startsWith("cid:"))
            return "<" + href.substring(4).trim() + ">"
        }
    }

    @Serializable
    @XmlSerialName("RunTime", xmlnsXI30, xmlnsXI30prefix)
    class RunTime(                      // Только в абапе
        @XmlValue(true)
        val content: MutableList<Element> = mutableListOf()
    )
}