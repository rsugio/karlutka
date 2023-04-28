package karlutka.parsers.pi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.OutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.mail.BodyPart
import javax.mail.internet.ContentType
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

const val xmlnsSOAP: String = "http://schemas.xmlsoap.org/soap/envelope/"
const val xmlnsSOAPprefix: String = "soap"
const val xmlnsXI30: String = "http://sap.com/xi/XI/Message/30"
const val xmlnsXI30prefix: String = "SAP"
const val xmlnsXLink: String = "http://www.w3.org/1999/xlink"

class XiMessage {
    enum class AckStatus {                     // com.sap.aii.af.sdk.xi.util.AckStatus
        OK, Error, AckRequestNotSupported
    }

    enum class AckCategory {                   // com.sap.aii.af.sdk.xi.util.AckCategory
        // permanent/transient for ABAP, null for Java
        permanent, transient
    }

    enum class PayloadType { //com.sap.aii.af.sdk.xi.util.PayloadType
        Application, ApplicationAttachment, System
    }

    enum class QOS { // com.sap.aii.af.sdk.xi.util.QualityOfService
        ExactlyOnce {
            override fun isAsync() = true
            override fun toShort() = "EO"
        },
        ExactlyOnceInOrder {
            override fun isAsync() = true
            override fun toShort() = "EOIO"
        },
        BestEffort {
            override fun isAsync() = false
            override fun toShort() = "BE"
        };

        abstract fun isAsync(): Boolean
        abstract fun toShort(): String
    }

    enum class MessageClass { // com.sap.aii.af.sdk.xi.util.MessageClass
        ApplicationMessage, ApplicationResponse, SystemAck, SystemError, ApplicationAck, ApplicationError
    }

    enum class ProcessingMode { // com.sap.aii.af.sdk.xi.util.ProcessingMode
        asynchronous, synchronous
    }

    enum class ErrorCategory {  // com.sap.aii.af.sdk.xi.util.ErrorCategory
        XIProtocol, Application, XIServer, XIAdapter, XIAdapterFramework, XIProxy
    }

    val mp: MimeMultipart
    private val ct: ContentType
    val boundary: String
    val payloadHrefs = mutableMapOf<String, Payload>()
    val payloadBodies = mutableMapOf<String, MimeBodyPart>()

    val header: Header?                 // нет для Fault
    val manifest: Manifest?
    val fault: Fault?
    private val _msid: String?          // используется только для вывода в случае непустого Header (нет вывода при Fault)
    private var writeFinished: Boolean = false

    constructor(h: Header) {
        this.mp = MimeMultipart("related")
        this.ct = ContentType(mp.contentType)
        this.boundary = ct.getParameter("boundary")

        this.header = h
        this.fault = null
        this.manifest = Manifest()
        // Если требуется выдать ответ с ошибкой как из абапа, то _msid можно делать пустым и не выдавать multipart/related
        this._msid = header.Main!!.MessageId.replace("-", "")
    }

    constructor(e: Envelope) {
        this.mp = MimeMultipart("related")
        this.ct = ContentType(mp.contentType)
        this.boundary = ct.getParameter("boundary")

        this.header = e.Header
        this.manifest = e.Body.Manifest
        this.fault = null
        this._msid = header!!.Main!!.MessageId.replace("-", "")
    }

//    constructor(f: Fault) {
//        this._msid = null
//        this.manifest = null
//        this.mp = MimeMultipart("related")
//        this.ct = ContentType(mp.contentType)
//        this.boundary = "" // ct.getParameter("boundary")
//
//        this.header = null
//        this.fault = f
//    }

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
            this.manifest = null
            // Ошибка
        } else {
            this.fault = null
            // Манифеста нет для успешного ответа если это Ack
            this.manifest = e.Body.Manifest
        }
    }

    fun getPayload(): BodyPart {
        requireNotNull(header)
        requireNotNull(manifest)
        requireNotNull(mp)
        val payload = manifest.Payload.find { it.Type == PayloadType.Application }
        requireNotNull(payload)
        val part = mp.getBodyPart(payload.getCid())
        requireNotNull(part, { payload.getCid() })
        return part
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

    fun systemAck(messageId: String, timeSent: String, status: AckStatus, category: AckCategory? = null): Envelope {
        val ack = Ack(status, category)
        val inpm = header!!.Main!!
        val main = Main(
            MessageClass.SystemAck, ProcessingMode.synchronous, messageId,
            inpm.MessageId,
            timeSent,
            PartyService(Party(null), ""),
            inpm.Sender,
            inpm.Interface
        )
        return Envelope(Header(main, null, null, ack), Body())
    }

    fun fault(faultcode: String = "soap:Server", faultstring: String, faultactor: String? = "http://sap.com/xi/XI/Message/30", err: Error): Envelope {
        val fault = Fault(faultcode, faultstring, faultactor, Detail(err))
        return Envelope(null, Body(null, fault))
    }

    fun syncResponse(messageId: String, timeSent: String): XiMessage {
        val inpm = header!!.Main!!
        val main = Main(
            MessageClass.ApplicationResponse, ProcessingMode.synchronous, messageId,
            inpm.MessageId,
            timeSent,
            PartyService(Party(null), ""),
            inpm.Sender,
            inpm.Interface
        )
        val rm = ReliableMessaging(QOS.BestEffort)
        val xiDC = DynamicConfiguration()
        val rez = Envelope(Header(main, rm, xiDC), Body(Manifest()))
        val xi = XiMessage(rez)
        return xi
    }

    fun syncError(messageId: String, timeSent: String, err: Error): XiMessage {
        val inpm = header!!.Main!!
        val main = Main(
            MessageClass.SystemError, ProcessingMode.synchronous, messageId,
            inpm.MessageId,
            timeSent,
            PartyService(Party(null), ""),
            inpm.Sender,
            inpm.Interface
        )
        val rm = ReliableMessaging(QOS.BestEffort)
        val xiDC = DynamicConfiguration()
        val rez = Envelope(Header(main, rm, xiDC, null, err), Body(Manifest()))
        val xi = XiMessage(rez)
        return xi
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
        return "multipart/related; boundary=\"$boundary\"; type=\"text/xml\"; start=\"<soap-$_msid@rsug.ru>\""
    }

    @Serializable
    @XmlSerialName("Envelope", xmlnsSOAP, xmlnsSOAPprefix)
    class Envelope(
        @XmlElement(true)
        val Header: Header? = null,             // нет для Fault
        @XmlElement(true)
        val Body: Body,
    ) {
        fun encodeToString() = xmlserializer.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("Body", xmlnsSOAP, xmlnsSOAPprefix)
    class Body(
        @XmlElement(true)
        val Manifest: Manifest? = null,          // нет для Ack и Fault
        @XmlElement(true)
        val Fault: Fault? = null
    )

    @Serializable
    @XmlSerialName("Fault", xmlnsSOAP, xmlnsSOAPprefix)
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
    @XmlSerialName("Header", xmlnsSOAP, xmlnsSOAPprefix)
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
        var Error: Error? = null,
        @XmlElement(true)
        var System: System? = null,
        @XmlElement(true)
        var Diagnostic: Diagnostic? = null,
        @XmlElement(true)
        var HopList: HopList? = null,
        @XmlElement(true)
        var Passport: Passport? = null,
        @XmlElement(true)
        @XmlSerialName("OutboundBinding", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        var OutboundBinding: CompactFragment? = null,           //в абапе есть, иначе нет
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
        @XmlSerialName("Category", xmlnsXI30, xmlnsXI30prefix)
        val Category: ErrorCategory,
        @XmlElement(true)
        val Code: ErrorCode,
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
        @XmlElement(true)
        val Retry: String? = null,      //M in ABAP, null otherwise
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("Code", xmlnsXI30, xmlnsXI30prefix)
    class ErrorCode(
        @XmlSerialName("area", "", "")
        val area: String,
        @XmlValue(true)
        val value: String
    )

    @Serializable
    @XmlSerialName("Ack", xmlnsXI30, xmlnsXI30prefix)
    class Ack(
        @XmlElement(true)
        @XmlSerialName("Status", xmlnsXI30, xmlnsXI30prefix)
        val Status: AckStatus,                     // com.sap.aii.af.sdk.xi.util.AckStatus - OK, Error, AckRequestNotSupported
        @XmlElement(true)
        @XmlSerialName("Category", xmlnsXI30, xmlnsXI30prefix)
        val Category: AckCategory? = null,           // com.sap.aii.af.sdk.xi.util.AckCategory - permanent for ABAP, null for Java, transient for ???
        val systemAckNotSupported: Boolean? = null,     // com.sap.aii.af.sdk.xi.util.AckAttribute
        val systemErrorAckNotSupported: Boolean? = null,
        val applicationAckNotSupported: Boolean? = null,
        val applicationErrorAckNotSupported: Boolean? = null,
        val arrivedAtFinalReceiver: Boolean? = null,    // заполняется в абапе при запросе Ack
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
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
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
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
        val Interface: Interface? = null,
        @XmlElement(true)
        val Mapping: Mapping? = null
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
    @XmlSerialName("Mapping", xmlnsXI30, xmlnsXI30prefix)
    class Mapping(
        val notRequired: String? = null
    )

    @Serializable
    @XmlSerialName("ReliableMessaging", xmlnsXI30, xmlnsXI30prefix)
    class ReliableMessaging(
        @XmlElement(true)
        @XmlSerialName("QualityOfService", xmlnsXI30, xmlnsXI30prefix)
        val QualityOfService: QOS,
        @XmlElement(true)
        val QueueId: String? = null,

        val ApplicationAckRequested: Boolean? = null,       //com.sap.aii.af.sdk.xi.util.AckRequestType но там везде с большой буквы
        val ApplicationErrorAckRequested: Boolean? = null,
        val SystemAckRequested: Boolean? = null,
        val SystemErrorAckRequested: Boolean? = null,

        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
        val soapMustUnderstand: Int = 1
    )

    @Serializable
    @XmlSerialName("System", xmlnsXI30, xmlnsXI30prefix)
    class System(
        @XmlElement(true)
        @XmlSerialName("Record", xmlnsXI30, xmlnsXI30prefix)
        val Record: MutableList<Record> = mutableListOf(),
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
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
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("HopList", xmlnsXI30, xmlnsXI30prefix)
    class HopList(
        @XmlElement(true)
        @XmlSerialName("Hop", xmlnsXI30, xmlnsXI30prefix)
        val HopList: MutableList<Hop> = mutableListOf(),
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
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
        @XmlSerialName("mustUnderstand", xmlnsSOAP, xmlnsSOAPprefix)
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
        @XmlSerialName("type", xmlnsXLink, "xlink")
        val xlinktype: String? = null, // simple. Пусто в абапе
        @XmlSerialName("href", xmlnsXLink, "xlink")
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

    companion object {
        private val xmlmodule = SerializersModule {
        }
        private val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = true
        }

        fun dateTimeSentNow() = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS))

    }
}