import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.junit.jupiter.api.Test;

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
        String content = "<routes>\n" +
                "<route>\n" +
                "    <from uri=\"timer:xml?period=1s\"/>\n" +
                "    <log message=\"------- Я тут -------\"/>\n" +
                "</route>\n" +
                "</routes>";
        String content2 = "<routes>\n" +
                "<route>\n" +
                "    <from uri=\"timer:xml2?period=2s\"/>\n" +
                "    <log message=\"------- Я там -------\"/>\n" +
                "</route>\n" +
                "</routes>";

        CamelContext context = new DefaultCamelContext();
        Resource resource = ResourceHelper.fromString("in-memory.xml", content);
        RouteBuilder builder = (RouteBuilder) new XmlRoutesBuilderLoader().loadRoutesBuilder(resource);
        context.addRoutes(builder);
        builder.configure();
        Resource resource2 = ResourceHelper.fromString("in-memory2.xml", content2);
        RouteBuilder builder2 = (RouteBuilder) new XmlRoutesBuilderLoader().loadRoutesBuilder(resource2);
        context.addRoutes(builder2);
        builder2.configure();

        context.start();
        Thread.sleep(10000L);
        context.stop();
    }

    @Test
    void camel4() throws Exception {
        String xmlDsl = "<routes>" +
                "<route id=\"0#грабитьКорованы\"><from uri=\"timer:thief?period=1s\"/><log message=\"------- Граблю -------\"/></route>" +
                "<route id=\"1#охранятьКорованы\"><from uri=\"timer:guard?period=845ms\"/><log message=\"------- Охраняю -------\"/></route>" +
                "</routes>";
        CamelContext camelContext = new DefaultCamelContext(true);
        Resource resource = ResourceHelper.fromString("memory.xml", xmlDsl);
        RouteBuilder builder = (RouteBuilder)(new XmlRoutesBuilderLoader().loadRoutesBuilder(resource));
        camelContext.addRoutes(builder);
        builder.configure();
        camelContext.start();
        System.out.println(camelContext.getRoutes());
        for (Route r: camelContext.getRoutes()) {
            System.out.println(r.getId());
            System.out.println(r.getDescription());
            System.out.println(r.getRoute());
            RouteDefinition rd = builder.getRouteCollection().getRoutes().get(0);
            System.out.println(r.toString());
            System.out.println(rd.toString());

        }
//        Thread.sleep(3000L);
        camelContext.stop();
    }
}
