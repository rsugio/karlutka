import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;

import java.net.URI;
import java.time.Instant;

// для проверки работы ideaj camel plugin
public class JCamel {

    static class R1 extends RouteBuilder {
        Processor p = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                for (InflightRepository.InflightExchange r: exchange.getContext().getInflightRepository().browse()) {
                    System.out.println(r);
                }
            }
        };
        public void configure() {
            from("file:/camel/src")
                    .autoStartup(true)
                    .routeId("R1")
                    .log("Hello from R1")
                    .choice()
                    .when(xpath("/person/city = 'Москва'"))
                    .log("Москва message")
                    .to("file:/camel/Москва")
                    .process(p)
                    .otherwise()
                    .log("Other message")
                    .to("file:/camel/НеМосква")
                    .process(p)
            ;
        }
    }

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();
        context.getInflightRepository().setInflightBrowseEnabled(true);
        context.addRoutes(new R1());

        //ProducerTemplate template = context.createProducerTemplate();
        Instant s = Instant.now();
        System.out.println("Started");
        context.start();
        Thread.sleep(2000L);
        context.stop();

        System.out.println(context.getInflightRepository().browse());

        long d = Instant.now().minusMillis(s.toEpochMilli()).toEpochMilli();
        System.out.println("Stopped in " + d + "ms");
    }

    public static void main2(String[] args) throws Exception {
        CamelContext camelContext = new DefaultCamelContext(true);

//        String from = "file:/camel?delete=false&recursive=false&exchangePattern=InOnly";
//        camelContext.addRoutes(new RouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                from(from);
//            }
//        });
//        Endpoint e = camelContext.getEndpoint("file:camel");
//        System.out.println(e);
//        if (true) return;


        String xmlDsl = """
            <route xmlns:s2="urn:s2" xmlns:s1="urn:s1" id="1">
              <from uri="file:/camel?delete=false"/>
              <description>ICo: |BC_TEST1|{urn:dummy-namespace}dummy_interface||, CC: test1sender</description>
              <log message="Файл взят"/>
              <setProperty name="icord0">
                <xpath resultType="BOOLEAN">boolean(/s1:a)</xpath>
              </setProperty>
              <setProperty name="icord1">
                <xpath resultType="BOOLEAN">/root='1' and /two='2' and /three='3' or /four='4' or /five='5' and boolean(/six)</xpath>
              </setProperty>
              <setProperty name="icord2">
                <xpath resultType="BOOLEAN">false or false and boolean(/c/d)</xpath>
              </setProperty>
              <log message="RD0=${exchangeProperty.icord0}"/>
              <log message="RD1=${exchangeProperty.icord1}"/>
              <log message="RD2=${exchangeProperty.icord2}"/>
            </route>""";

        Resource resource = ResourceHelper.fromString("memory.xml", xmlDsl);
        RouteBuilder builder = (RouteBuilder) (new XmlRoutesBuilderLoader().loadRoutesBuilder(resource));
        camelContext.addRoutes(builder);
        builder.configure();
        camelContext.start();
        Thread.sleep(2000L);
        camelContext.stop();

    }
}
