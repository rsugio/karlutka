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
        val cr2 = CommunicationChannelReadResponse.parseSOAP(s("/pi_XiBasis/03CommunicationChannelReadResponse2.xml"), fault)
        require(fault.isSuccess() && !cr2!!.LogMessageCollection.channelLogs.isEmpty())
        val sc = ConfigurationScenarioQueryResponse.parseSOAP(s("/pi_XiBasis/04ConfigurationScenarioQueryResponse.xml "), fault)
        require(fault.isSuccess() && !sc!!.ConfigurationScenarioID.isEmpty())
        val sr = ConfigurationScenarioReadResponse.parseSOAP(s("/pi_XiBasis/05ConfigurationScenarioReadResponse.xml"), fault)
        require(fault.isSuccess() && !sr!!.ConfigurationScenario.isEmpty())
        val ico = IntegratedConfiguration750ReadResponse.parseSOAP(s("/pi_XiBasis/06IntegratedConfiguration750ReadResponse.xml"), fault)
        require(fault.isSuccess() && !ico!!.IntegratedConfiguration.isEmpty())
        val ico2 = IntegratedConfiguration750ReadResponse.parseSOAP(s("/pi_XiBasis/07IntegratedConfiguration750ReadResponse2.xml"), fault)
        require(fault.isSuccess() && !ico2!!.IntegratedConfiguration.isEmpty())
        val ico3 = IntegratedConfigurationQueryResponse.parseSOAP(s("/pi_XiBasis/08IntegratedConfigurationQueryResponse.xml"), fault)
        require(fault.isSuccess() && !ico3!!.IntegratedConfigurationID.isEmpty())
        val ico4 = IntegratedConfigurationReadResponse.parseSOAP(s("/pi_XiBasis/09IntegratedConfigurationReadResponse.xml"), fault)
        require(fault.isSuccess() && !ico4!!.IntegratedConfiguration.isEmpty())
        val vm1 = ValueMappingQueryResponse.parseSOAP(s("/pi_XiBasis/10ValueMappingQueryResponse.xml"), fault)
        require(fault.isSuccess() && !vm1!!.ValueMappingID.isEmpty())
        val vm2 = ValueMappingReadResponse.parseSOAP(s("/pi_XiBasis/11ValueMappingReadResponse.xml"), fault)
        require(fault.isSuccess() && !vm2!!.ValueMapping.isEmpty())

    }
}