package karlutka.parsers.pi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

const val xmlnsSOAP: String = "http://schemas.xmlsoap.org/soap/envelope/"
const val xmlnsXI30: String = "http://sap.com/xi/XI/Message/30"
const val xmlnsXI30prefix: String = "SAP"

class XiMessage {
    enum class PayloadType {Application, ApplicationAttachment}
    enum class QOS {ExactlyOnce, ExactlyOnceInOrder}

    @Serializable
    @XmlSerialName("Envelope", xmlnsSOAP, "SOAP")
    class Envelope(
        @XmlElement(true)
        val Header: Header,
        @XmlElement(true)
        val Body: Body,
    )

    @Serializable
    @XmlSerialName("Header", xmlnsSOAP, "SOAP")
    class Header(
        @XmlElement(true)
        val Main: Main,
        @XmlElement(true)
        val ReliableMessaging: ReliableMessaging,
        @XmlElement(true)
        val DynamicConfiguration: DynamicConfiguration? = null,
        @XmlElement(true)
        val System: System? = null,
        @XmlElement(true)
        val Diagnostic: Diagnostic? = null,
        @XmlElement(true)
        val HopList: HopList? = null,
        @XmlElement(true)
        val Passport: Passport? = null,
        @XmlElement(true)
        @XmlSerialName("RunTime", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        val RunTime: CompactFragment? = null,
        @XmlElement(true)
        @XmlSerialName("PerformanceHeader", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        val PerformanceHeader: CompactFragment? = null,
        @XmlElement(true)
        @XmlSerialName("Trace", xmlnsXI30, xmlnsXI30prefix)
        @Contextual
        val Trace: CompactFragment? = null,
    )

    @Serializable
    @XmlSerialName("Body", xmlnsSOAP, "SOAP")
    class Body(
        @XmlElement(true)
        val Manifest: Manifest,
    )

    @Serializable
    @XmlSerialName("Main", xmlnsXI30, xmlnsXI30prefix)
    class Main(
        val versionMajor: Int = 3,
        val versionMinor: Int = 1,
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
        @XmlElement(true)
        val MessageClass: String, // = "ApplicationMessage",
        @XmlElement(true)
        val ProcessingMode: String, // = "asynchronous",
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
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1
    )

    @Serializable
    @XmlSerialName("System", xmlnsXI30, xmlnsXI30prefix)
    class System(
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        @XmlSerialName("Record", xmlnsXI30, xmlnsXI30prefix)
        val Record: List<Record>
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
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        val TraceLevel: String,
        @XmlElement(true)
        val Logging: String,
    )

    @Serializable
    @XmlSerialName("HopList", xmlnsXI30, xmlnsXI30prefix)
    class HopList(
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        @XmlSerialName("Hop", xmlnsXI30, xmlnsXI30prefix)
        val HopList: List<Hop>
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
        val Record: List<Record>,
        @XmlSerialName("mustUnderstand", xmlnsSOAP, "SOAP")
        val soapMustUnderstand: Int = 1,
    )

    @Serializable
    @XmlSerialName("Manifest", xmlnsXI30, xmlnsXI30prefix)
    class Manifest(
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
        @XmlElement(true)
        @XmlSerialName("Payload", xmlnsXI30, xmlnsXI30prefix)
        val Payload: List<Payload>
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
    )

    @Serializable
    @XmlSerialName("RunTime", xmlnsXI30, xmlnsXI30prefix)
    class RunTime(                      // Только в абапе
        @XmlValue(true)
        val content: List<Element>
    )
}