import k5.*;
import k6.Blueprint;
import k6.Definitions;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Tests {
    String localhost = "http://localhost";
    String getString(String resourceName) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is == null) throw new IOException("Not found resource: " + resourceName);
        String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();
        return text;
    }

    void printSoap(String url, String soapXml) {
//        System.out.println(url);
//        System.out.println(soapXml+"\n\n");
    }

    @Test
    public void pi() throws IOException {
        // Value Mappings
        printSoap(ValueMappingQueryRequest.Companion.getUrl(localhost),
                new ValueMappingQueryRequest().composeSOAP());
        ValueMappingQueryResponse vmqr = ValueMappingQueryResponse.Companion.parse(getString("/PI_xiBasis/ValueMappingQueryResponse.xml"));
        printSoap(ValueMappingReadRequest.Companion.getUrl(localhost),
                new ValueMappingReadRequest(null, vmqr.getValueMappingID()).composeSOAP());
        ValueMappingReadResponse vmrr = ValueMappingReadResponse.Companion.parse(getString("/PI_xiBasis/ValueMappingReadResponse.xml"));
        assert vmrr.getValueMapping().size() > 0;

        // Communication Channels
        printSoap(CommunicationChannelQueryRequest.Companion.getUrl(localhost),
                new CommunicationChannelQueryRequest().composeSOAP());
        CommunicationChannelQueryResponse ccqr = CommunicationChannelQueryResponse.Companion.parse(getString("/PI_xiBasis/CommunicationChannelQueryResponse.xml"));
        printSoap(CommunicationChannelReadRequest.Companion.getUrl(localhost),
                new CommunicationChannelReadRequest("User", ccqr.getChannels()).composeSOAP());
        CommunicationChannelReadResponse ccrr = CommunicationChannelReadResponse.Companion.parse(getString("/PI_xiBasis/CommunicationChannelReadResponse.xml"));
        assert ccrr.getChannels().size() > 0;
        ccrr = CommunicationChannelReadResponse.Companion.parse(getString("/PI_xiBasis/CommunicationChannelReadResponse2.xml"));
        assert ccrr.getChannels().size() == 0 && ccrr.getLogMessageCollection().getChannelLogs().size()>0;

        // Configuration Scenarios
        printSoap(ConfigurationScenarioQueryRequest.Companion.getUrl(localhost),
                new ConfigurationScenarioQueryRequest().composeSOAP());
        ConfigurationScenarioQueryResponse csqr = ConfigurationScenarioQueryResponse.Companion.parse(getString("/PI_xiBasis/ConfigurationScenarioQueryResponse.xml"));
        printSoap(ConfigurationScenarioReadRequest.Companion.getUrl(localhost),
                new ConfigurationScenarioReadRequest("User", csqr.getConfigurationScenarioID()).composeSOAP());
        ConfigurationScenarioReadResponse csrr = ConfigurationScenarioReadResponse.Companion.parse(getString("/PI_xiBasis/ConfigurationScenarioReadResponse.xml"));
        assert csrr.getConfigurationScenario().size() > 0;

        // ICo
        printSoap(IntegratedConfigurationQueryRequest.Companion.getUrl750(localhost),
                new IntegratedConfigurationQueryRequest().composeSOAP());
        IntegratedConfigurationQueryResponse icqr = IntegratedConfigurationQueryResponse.Companion.parse(getString("/PI_xiBasis/IntegratedConfigurationQueryResponse.xml"));
        printSoap(IntegratedConfigurationReadRequest.Companion.getUrl750(localhost),
                new IntegratedConfigurationReadRequest("User", icqr.getIntegratedConfigurationID()).composeSOAP());
        IntegratedConfiguration750ReadResponse icrr = IntegratedConfiguration750ReadResponse.Companion.parse(getString("/PI_xiBasis/IntegratedConfiguration750ReadResponse.xml"));
        assert icrr.getIntegratedConfiguration().size() > 0;
        icrr = IntegratedConfiguration750ReadResponse.Companion.parse(getString("/PI_xiBasis/IntegratedConfiguration750ReadResponse2.xml"));
        assert icrr.getIntegratedConfiguration().size() > 0;
    }

    @Test
    public void cpi() throws IOException {
        Definitions.Companion.parse(getString("/Iflow/1.iflw.xml"));
//      пока блупринты не парсятся полностью, надо писать дальше
        if (false) {
            Blueprint.Companion.parse(getString("/Iflow/beans.xml"));
        }
    }
}
