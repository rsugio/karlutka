import karlutka.parsers.pi.XiMessage
import karlutka.serialization.KSoap
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class KXiMessageTest {
    @Test
    fun constructor() {
        val sender = XiMessage.XPartyService(XiMessage.XParty("agency","schema","value"), "DER200")
        val receiver = XiMessage.XPartyService(XiMessage.XParty("agency","schema","value"), "BS_DigitalPlatformKafka_D")
        val iface = XiMessage.XInterface("urn:nlmk:po:I:DIGITALPLATFORMKAFKA:India:CharTransfering", "SI_CharTransfering_InAsync")
        val mes = XiMessage.SapMain("3","1", "1", null, "ApplicationMessage", "asynchronous", "005056bf-38e0-1edd-b58c-9b5814a5e450", "2023-04-06T10:22:01Z",
            sender, receiver, iface)
        val smes = KSoap.xmlserializer.encodeToString(mes)
        println(smes)
    }
}