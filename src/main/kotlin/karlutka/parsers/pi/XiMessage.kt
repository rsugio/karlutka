package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class XiMessage {
    @Serializable
    @XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
    class XSOAP(
        @XmlElement(true)
        val Header: XHeader,
        @XmlElement(true)
        val Body: XBody
    )

    @Serializable
    @XmlSerialName("Header", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
    class XHeader(
        @XmlElement(true)
        val Main: XMain,
        @XmlElement(true)
        val ReliableMessaging: XReliableMessaging,
        @XmlElement(true)
        val System: XSystem,
        @XmlElement(true)
        val Diagnostic: XDiagnostic,
        @XmlElement(true)
        val HopList: XHopList,
        @XmlElement(true)
        val Passport: XPassport? = null,
        @XmlElement(true)
        val DynamicConfiguration: XDynamicConfiguration,
    )

    @Serializable
    @XmlSerialName("Body", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
    class XBody(
        @XmlElement(true)
        val Manifest: XManifest,
    )

    @Serializable
    @XmlSerialName("Main", "http://sap.com/xi/XI/Message/30", "SAP")
    class XMain(
        val versionMajor: Int = 3,
        val versionMinor: Int = 1,
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
        @XmlElement(true)
        val MessageClass: String = "ApplicationMessage",
        @XmlElement(true)
        val ProcessingMode: String = "asynchronous",
        @XmlElement(true)
        val MessageId: String = "005056bf-38e0-1edd-b58c-9b5814a5e450",
        @XmlElement(true)
        val TimeSent: String = "2023-04-06T10:22:01Z",
        @XmlElement(true)
        @XmlSerialName("Sender", "http://sap.com/xi/XI/Message/30", "SAP")
        val Sender: XPartyService,
        @XmlElement(true)
        @XmlSerialName("Receiver", "http://sap.com/xi/XI/Message/30", "SAP")
        val Receiver: XPartyService,
        @XmlElement(true)
        val Interface: XInterface,
    )

    @Serializable
    class XPartyService(
        @XmlElement(true)
        val Party: XParty,
        @XmlElement(true)
        val Service: String
    )

    @Serializable
    @XmlSerialName("Party", "http://sap.com/xi/XI/Message/30", "SAP")
    class XParty(
        val agency: String = "http://sap.com/xi/XI",
        val scheme: String = "XIParty",
        @XmlValue(true)
        val content: String = ""
    )

    @Serializable
    @XmlSerialName("Interface", "http://sap.com/xi/XI/Message/30", "SAP")
    class XInterface(
        val namespace: String,
        @XmlValue(true)
        val content: String = ""
    )

    @Serializable
    @XmlSerialName("ReliableMessaging", "http://sap.com/xi/XI/Message/30", "SAP")
    class XReliableMessaging(
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        val QualityOfService: String = "ExactlyOnce"
    )

    @Serializable
    @XmlSerialName("System", "http://sap.com/xi/XI/Message/30", "SAP")
    class XSystem(
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        @XmlSerialName("Record", "http://sap.com/xi/XI/Message/30", "SAP")
        val Record: List<XRecord>
    )

    @Serializable
    @XmlSerialName("Record", "http://sap.com/xi/XI/Message/30", "SAP")
    class XRecord(
        val namespace: String,
        val name: String,
        @XmlValue(true)
        val content: String? = null
    )

    @Serializable
    @XmlSerialName("Diagnostic", "http://sap.com/xi/XI/Message/30", "SAP")
    class XDiagnostic(
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        val TraceLevel: String,
        @XmlElement(true)
        val Logging: String,
    )

    @Serializable
    @XmlSerialName("HopList", "http://sap.com/xi/XI/Message/30", "SAP")
    class XHopList(
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        @XmlSerialName("Hop", "http://sap.com/xi/XI/Message/30", "SAP")
        val HopList: List<XHop>
    )

    @Serializable
    class XHop(
        val timeStamp: String,
        val wasRead: Boolean,
        @XmlElement(true)
        val Engine: XEngine,
        @XmlElement(true)
        val Adapter: XAdapter,
        @XmlElement(true)
        val MessageId: String,
        @XmlElement(true)
        val Info: String? = null,
    )

    @Serializable
    @XmlSerialName("Engine", "http://sap.com/xi/XI/Message/30", "SAP")
    class XEngine(
        val type: String = "BS",
        @XmlValue(true)
        val content: String
    )

    @Serializable
    @XmlSerialName("Adapter", "http://sap.com/xi/XI/Message/30", "SAP")
    class XAdapter(
        val namespace: String = "http://sap.com/xi/XI/System",
        @XmlValue(true)
        val content: String = "XI"
    )

    @Serializable
    @XmlSerialName("Passport", "http://sap.com/xi/XI/Message/30", "SAP")
    class XPassport(
        @XmlElement(true)
        val PassportHash: String,
        @XmlElement(true)
        val Transaction_ID: String
    )

    @Serializable
    @XmlSerialName("DynamicConfiguration", "http://sap.com/xi/XI/Message/30", "SAP")
    class XDynamicConfiguration(
        @XmlSerialName("mustUnderstand", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: Int = 1,
        @XmlElement(true)
        val Record: List<XRecord>
    )

    @Serializable
    @XmlSerialName("Manifest", "http://sap.com/xi/XI/Message/30", "SAP")
    class XManifest(
        @XmlSerialName("Id", "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu")
        val wsuId: String? = null,
        @XmlElement(true)
        val Payload: XPayload
    )

    @Serializable
    @XmlSerialName("Payload", "http://sap.com/xi/XI/Message/30", "SAP")
    class XPayload(
        @XmlSerialName("type", "http://www.w3.org/1999/xlink", "xlink")
        val xlinktype: String, // simple
        @XmlSerialName("href", "http://www.w3.org/1999/xlink", "xlink")
        val href: String, // cid:payload-005056BF38E01EDDB58C9B5814A60450@sap.com
        @XmlElement(true)
        val Name: String,
        @XmlElement(true)
        val Description: String,
        @XmlElement(true)
        val Type: String,
    )
}