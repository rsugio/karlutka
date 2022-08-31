import KT.Companion.x
import karlutka.parsers.pi.XiBasis.*
import karlutka.serialization.KSoap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.nio.file.Paths
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test

@OptIn(ExperimentalSerializationApi::class)
class KXiBasisTests {
    @Test
    fun statics() {
        val fault = KSoap.Fault()
        val s = CommunicationChannelQueryRequest().composeSOAP()
        require(s.isNotBlank())

        val cc = KSoap.parseSOAP<CommunicationChannelQueryResponse>(
            x("/pi_XiBasis/01CommunicationChannelQueryResponse.xml"),
            fault
        )
        require(fault.isSuccess() && cc!!.channels.isNotEmpty())

        val cr = KSoap.parseSOAP<CommunicationChannelReadResponse>(
            x("/pi_XiBasis/02CommunicationChannelReadResponse.xml"),
            fault
        )
        require(fault.isSuccess() && cr!!.channels.isNotEmpty())
        val cr2 = KSoap.parseSOAP<CommunicationChannelReadResponse>(
            x("/pi_XiBasis/03CommunicationChannelReadResponse2.xml"),
            fault
        )
        require(fault.isSuccess() && cr2!!.LogMessageCollection.channelLogs.isNotEmpty())
        val sc = KSoap.parseSOAP<ConfigurationScenarioQueryResponse>(
            x("/pi_XiBasis/04ConfigurationScenarioQueryResponse.xml "),
            fault
        )
        require(fault.isSuccess() && sc!!.ConfigurationScenarioID.isNotEmpty())
        val sr = KSoap.parseSOAP<ConfigurationScenarioReadResponse>(
            x("/pi_XiBasis/05ConfigurationScenarioReadResponse.xml"),
            fault
        )
        require(fault.isSuccess() && sr!!.ConfigurationScenario.isNotEmpty())
        val ico = KSoap.parseSOAP<IntegratedConfiguration750ReadResponse>(
            x("/pi_XiBasis/06IntegratedConfiguration750ReadResponse.xml"),
            fault
        )
        require(fault.isSuccess() && ico!!.IntegratedConfiguration.isNotEmpty())
        val ico2 = KSoap.parseSOAP<IntegratedConfiguration750ReadResponse>(
            x("/pi_XiBasis/07IntegratedConfiguration750ReadResponse2.xml"),
            fault
        )
        require(fault.isSuccess() && ico2!!.IntegratedConfiguration.isNotEmpty())
        val ico3 = KSoap.parseSOAP<IntegratedConfigurationQueryResponse>(
            x("/pi_XiBasis/08IntegratedConfigurationQueryResponse.xml"),
            fault
        )
        require(fault.isSuccess() && ico3!!.IntegratedConfigurationID.isNotEmpty())
        val ico4 = KSoap.parseSOAP<IntegratedConfigurationReadResponse>(
            x("/pi_XiBasis/09IntegratedConfigurationReadResponse.xml"),
            fault
        )
        require(fault.isSuccess() && ico4!!.IntegratedConfiguration.isNotEmpty())
        val vm1 = KSoap.parseSOAP<ValueMappingQueryResponse>(x("/pi_XiBasis/10ValueMappingQueryResponse.xml"), fault)
        require(fault.isSuccess() && vm1!!.ValueMappingID.isNotEmpty())
        val vm2 = KSoap.parseSOAP<ValueMappingReadResponse>(x("/pi_XiBasis/11ValueMappingReadResponse.xml"), fault)
        require(fault.isSuccess() && vm2!!.ValueMapping.isNotEmpty())
    }

    @Test
    fun cbor() {
        val fault = KSoap.Fault()
        val cr = KSoap.parseSOAP<CommunicationChannelReadResponse>(
            x("/pi_XiBasis/02CommunicationChannelReadResponse.xml"),
            fault
        )
        val o = cr!!.channels[0]
        val bytes = Cbor.encodeToByteArray(o)
        val p = Paths.get("c:\\data\\tmp\\cbor1.bin")
        p.writeBytes(bytes)
        val o2 = Cbor.decodeFromByteArray<CommunicationChannel>(p.readBytes())
        println(o2)
    }
}