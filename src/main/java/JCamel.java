import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

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

    public static void main(String[] args) throws Exception {
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
}
