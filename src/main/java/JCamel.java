import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;

import java.time.Instant;

// для проверки работы ideaj camel plugin
public class JCamel {
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

    public static void main1(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new R1());

        //ProducerTemplate template = context.createProducerTemplate();
        Instant s = Instant.now();
        System.out.println("Started");
        context.start();
        Thread.sleep(2000L);
        context.stop();
        long d = Instant.now().minusMillis(s.toEpochMilli()).toEpochMilli();
        System.out.println("Stopped in " + d + "ms");
    }

    public static void main(String[] args) throws Exception {
        String xmlDsl = "<routes>" +
                "<route id=\"0#грабитьКорованы\"><from uri=\"timer:thief?period=10s\"/><log message=\"------- Граблю -------\"/></route>" +
                "<route id=\"1#охранятьКорованы\"><from uri=\"timer:guard?period=2345ms\"/><log message=\"------- Охраняю -------\"/></route>" +
                "</routes>";
        CamelContext camelContext = new DefaultCamelContext(true);
        Resource resource = ResourceHelper.fromString("memory.xml", xmlDsl);
        RouteBuilder builder = (RouteBuilder)(new XmlRoutesBuilderLoader().loadRoutesBuilder(resource));
        camelContext.addRoutes(builder);
        builder.configure();
        camelContext.start();
        Thread.sleep(2000L);
        camelContext.stop();

    }
}
