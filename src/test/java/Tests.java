import k5.*;
import k6.Blueprint;
import k6.Definitions;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Tests {
    String getString(String resourceName) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is == null) throw new IOException("Not found resource: " + resourceName);
        String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();
        return text;
    }

    @Test
    public void all() throws IOException {
        Definitions.Companion.parse(getString("/Iflow/1.iflw.xml"));
//        Blueprint.Companion.parse(getString("/Iflow/beans.xml"));
        ValueMappingQueryResponse vmqr = ValueMappingQueryResponse.Companion.parse(getString("/PI_xiBasis/ValueMappingQueryResponse.xml"));
        ValueMappingReadResponse vmrr = ValueMappingReadResponse.Companion.parse(getString("/PI_xiBasis/ValueMappingReadResponse.xml"));
        CommunicationChannelQueryResponse ccqr = CommunicationChannelQueryResponse.Companion.parse(getString("/PI_xiBasis/CommunicationChannelQueryResponse.xml"));
        CommunicationChannelReadResponse ccrr = CommunicationChannelReadResponse.Companion.parse(getString("/PI_xiBasis/CommunicationChannelReadResponse.xml"));
        ConfigurationScenarioQueryResponse csqr = ConfigurationScenarioQueryResponse.Companion.parse(getString("/PI_xiBasis/ConfigurationScenarioQueryResponse.xml"));
        ConfigurationScenarioReadResponse csrr = ConfigurationScenarioReadResponse.Companion.parse(getString("/PI_xiBasis/ConfigurationScenarioReadResponse.xml"));
    }
}
