import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.xml.io.CamelXmlRoutesBuilderLoader;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.ResourceSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;

public class JCamelTest {
    static class R1 extends RouteBuilder {
        public void configure() {
            from("file:/camel/src")
                    .autoStartup(true)
                    .routeId("R1")
                    .log("Hello from R1")
                    .choice()
                    .when(xpath("/person/city = 'Москва'"))
                    .log("Москва message")
                    .to("file:/camel/Москва")
                    .otherwise()
                    .log("Other message")
                    .to("file:/camel/НеМосква");
        }
    }

    @Test
    void camel1() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new R1());
        ProducerTemplate template = context.createProducerTemplate();
        Instant s = Instant.now();
        System.out.println("Started");
        context.start();
        Thread.sleep(2000L);
        context.stop();
        long d = Instant.now().minusMillis(s.toEpochMilli()).toEpochMilli();
        System.out.println("Stopped in " + d + "ms");
    }
    @Test
    void camel2() throws Exception {
        String r2 = "<route>" +
                "<from uri=\"file:/camel/src\"/>" +
                "<to uri=\"file:/camel/Прочее\"/>" +
                "</route>";
        CamelContext context = new DefaultCamelContext();
        InputStream is = getClass().getResourceAsStream("/camel/barRoute.xml");
        assert(is!=null);
        XmlRoutesBuilderLoader l = new XmlRoutesBuilderLoader();
        //l.loadRoutesBuilder(getClass().getResource("/camel/barRoute.xml"));

    }
}
