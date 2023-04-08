import KT.Companion.x
import karlutka.parsers.pi.XiMessage
import karlutka.serialization.KSoap
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class KXiMessageTest {
    @Test
    fun constructor() {
        val sender = XiMessage.XPartyService(XiMessage.XParty("agency", "schema", ""), "DER200")
        val receiver = XiMessage.XPartyService(XiMessage.XParty("agency", "schema", ""), "BS_DigitalPlatformKafka_D")
        val iface = XiMessage.XInterface("urn:vendor:po:I:DIGITALPLATFORMKAFKA:India:CharTransfering", "SI_CharTransfering_InAsync")
        val main = XiMessage.XMain(
            3, 1, 1, null, "ApplicationMessage", "asynchronous", "005056bf-38e0-1edd-b58c-9b5814a5e450", "2023-04-06T10:22:01Z",
            sender, receiver, iface
        )
        val smain = KSoap.xmlserializer.encodeToString(main)

        val srecord = KSoap.xmlserializer.encodeToString(XiMessage.XRecord("http://sap.com/xi/XI/Message/30/general", "interfaceDeterminationGUID", "830791ead46411edb906000000417ca6"))
        println(srecord)

    }

    @Test
    fun parse() {
        val xml = x("/pi_XI/message1.xml")
        val z = KSoap.xmlserializer.parse<XiMessage.XSOAP>(xml)
    }
}