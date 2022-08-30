import KT.Companion.s
import karlutka.parsers.pi.XiBasis.*
import karlutka.serialization.KSoap
import kotlin.test.Test

class KXiBasisTests {
    @Test
    fun statics() {
        val fault = KSoap.Fault()
        val s = CommunicationChannelQueryRequest().composeSOAP()
        require(s.isNotBlank())

        val cc = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
        require(fault.isSuccess() && !cc!!.channels.isEmpty())
        val cr = CommunicationChannelReadResponse.parseSOAP(s("/pi_XiBasis/02CommunicationChannelReadResponse.xml"), fault)
        require(fault.isSuccess() && !cr!!.channels.isEmpty())
//        val a = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
//        require(fault.isSuccess() && !a!!.channels.isEmpty())
//        val a = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
//        require(fault.isSuccess() && !a!!.channels.isEmpty())
//        val a = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
//        require(fault.isSuccess() && !a!!.channels.isEmpty())
//        val a = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
//        require(fault.isSuccess() && !a!!.channels.isEmpty())
//        val a = CommunicationChannelQueryResponse.parseSOAP(s("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"), fault)
//        require(fault.isSuccess() && !a!!.channels.isEmpty())


    }
}