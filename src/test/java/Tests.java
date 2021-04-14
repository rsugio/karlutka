import k1.HmAttribute;
import k1.HmInstance;
import k1.NotSoComplexQuery;
import k3.HttpAccessLogLine;
import k3.LjsTraceLine;
import k5.*;
import k6.Blueprint;
import k6.IFlowBpmnDefinitions;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Tests {
    String localhost = "http://localhost";

    String getString(String resourceName) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is == null) throw new IOException("Not found resource: " + resourceName);
        String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();
        return text;
    }

    Scanner getScanner(String resourceName) {
        InputStream is = getClass().getResourceAsStream(resourceName);
        return new Scanner(is);
    }

    DirectoryStream<Path> getDirectoryStream(String folder, String glob)
            throws URISyntaxException, IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        assert url != null;
        return Files.newDirectoryStream(Paths.get(url.toURI()), glob);
    }

    void printSoap(String url, String soapXml) {
        //noinspection ConstantConditions
        if (1 > 0) {
            System.out.println(url);
            System.out.println(soapXml + "\n\n");
        }
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
        assert ccrr.getChannels().size() == 0 && ccrr.getLogMessageCollection().getChannelLogs().size() > 0;

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

        // before 750
        printSoap(IntegratedConfigurationQueryRequest.Companion.getUrl(localhost),
                new IntegratedConfigurationQueryRequest().composeSOAP());
        printSoap(IntegratedConfigurationReadRequest.Companion.getUrl(localhost),
                new IntegratedConfigurationReadRequest("User", icqr.getIntegratedConfigurationID()).composeSOAP());
        IntegratedConfigurationReadResponse icrr2 = IntegratedConfigurationReadResponse.Companion.parse(getString("/PI_xiBasis/IntegratedConfigurationReadResponse.xml"));
        assert icrr2.getIntegratedConfiguration().size() > 0;
    }

    @Test
    public void simpleQuery() throws Exception {
        NotSoComplexQuery.Companion.getAllowedRep();
        NotSoComplexQuery.Companion.getUrlRep("http://localhost");
        NotSoComplexQuery.Companion.getUrlDir("http://localhost");
        NotSoComplexQuery.Companion.getContentType();
        NotSoComplexQuery.Companion.repQuery("XI_TRAFO");

        NotSoComplexQuery n;
//        n = new NotSoComplexQuery(getScanner("SimpleQuery/rep_ifmmessif.html"));
//        System.out.println(n.getHeaders() + "\t" + n.getLines().size());
        n = new NotSoComplexQuery(getScanner("SimpleQuery/XI_TRAFO.html"));
        System.out.println(n.getHeaders() + "\t" + n.getLines().size());
        for (Map<String, String> m : n.getLines()) {
            String name = m.get("Name");
            String desc = m.get("Description");
        }
    }

    @Test
    public void hmi() throws Exception {
        for (Path x : getDirectoryStream("Hmi", "*.xml")) {
            String text = Files.readString(x);
            HmInstance obj = HmInstance.Companion.parse(text);
            for (HmAttribute attr : obj.getAttribute()) {
                System.out.println(attr);
            }
        }
    }

    @Test
    public void hmValue() {
        HmInstance inst = new HmInstance("typeId", new ArrayList<HmAttribute>());
//        HmValue val = new HmValue(-1, true, inst);
//        System.out.println(val.printXml());
//        String s = val.printXml();
//        System.out.println(s);
//        HmValue val2 = HmValue.Companion.parse(s);
//        System.out.println(val2.getData());
    }

    @Test
    public void cpi() throws Exception {
        for (Path x : getDirectoryStream("Iflow", "*.iflw")) {
            String text = Files.readString(x);
            IFlowBpmnDefinitions.Companion.parse(text);
        }
        //noinspection ConstantConditions
        if (false) {
            Blueprint.Companion.parse(getString("/Iflow/beans.xml"));
        }
    }

    @Test
    public void cpiLogsAccessLogs() throws Exception {
        HttpAccessLogLine.Companion.toZoned("11/Apr/2099:23:59:59 +0130");
        List<HttpAccessLogLine> all = new ArrayList<>(1024);
        for (Path x : getDirectoryStream("CpiLogs", "http_access*.*")) {
            System.out.println(x);
            if (x.toString().endsWith(".zip")) {
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(x));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    Scanner sc = new Scanner(zis);
                    all.addAll(HttpAccessLogLine.Companion.parse(sc));
                    sc.close();
                    ze = zis.getNextEntry();
                }
                zis.close();
            } else if (x.toString().endsWith(".gz")) {
                GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(x));
                Scanner sc = new Scanner(gz);
                all.addAll(HttpAccessLogLine.Companion.parse(sc));
                sc.close();
                gz.close();
            }
        }
        System.out.println(HttpAccessLogLine.Companion.distinct(all));
    }

    @Test
    public void cpiLjsTraces() throws Exception {
        ZonedDateTime zdt = LjsTraceLine.Companion.toZoned("2021 04 14 23:59:59#+02#");
        System.out.println(zdt);
        List<LjsTraceLine> all = new ArrayList<>(1024);
        for (Path x : getDirectoryStream("CpiLogs", "ljs*.*")) {
            System.out.println(x);
            if (x.toString().endsWith(".gz")) {
                GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(x));
                all.addAll(LjsTraceLine.Companion.parse(gz));
                gz.close();
            }
        }
        System.out.println(LjsTraceLine.Companion.distinct(all));
    }

    @Test
    public void sapcontrol() throws Exception {
        printSoap(SAPControl.Companion.getUrl13(localhost),
                new SAPControl.ListLogFiles().composeSOAP());
        SAPControl.ListLogFilesResponse llfr = SAPControl.ListLogFilesResponse.Companion.parseSOAP(getString("/SAPControl/ListLogFilesResponse.xml"));
        for (SAPControl.ListLogFilesResponse.Item i : llfr.getFile().getItem()) {
            System.out.println(i.getFilename() + "\t" + i.getFormat());
        }
    }
}
