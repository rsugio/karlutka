import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.junit.jupiter.api.Test

class KCamelTest {
    @Test
    fun a() {
        val context: CamelContext = DefaultCamelContext()
        context.addRoutes(object : RouteBuilder() {
            override fun configure() {
                from("direct:test")
                    .to("log")
            }
        })
        println(context)
        val template = context.createProducerTemplate()
        println(template)
        context.start()
        context.stop()
        println(context)

    }
}