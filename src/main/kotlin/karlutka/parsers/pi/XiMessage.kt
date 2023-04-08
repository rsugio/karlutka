package karlutka.parsers.pi

import karlutka.serialization.KSoap
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class XiMessage {
    @Serializable
    @XmlSerialName("Main", "http://sap.com/xi/XI/Message/30", "SAP")
    class SapMain(
        val versionMajor: String = "3",
        val versionMinor: String = "1",
        @XmlSerialName("SOAP", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP")
        val soapMustUnderstand: String = "1",
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
        val Sender: XPartyService,
        @XmlElement(true)
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
    class XParty(
        val agency: String = "http://sap.com/xi/XI",
        val schema: String = "XIParty",
        @XmlValue(true)
        val content: String = ""
    )
    @Serializable
    class XInterface(
        val namespace: String,
        @XmlValue(true)
        val content: String = ""
    )


}