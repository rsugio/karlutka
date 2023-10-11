import org.junit.jupiter.api.Test
import ru.rsug.karlutka.nifi.NiFi
import java.io.FileInputStream
import java.nio.file.Paths

class KNiFi {
    private val predicate = { x: Map.Entry<String, String> ->
        val banned = listOf("", "PO.Password", "S3.SecretKey", "Email.From", "Email.To", "Email.Subject", "mime.type")
        !banned.contains(x.key) && !x.value.endsWith("/RESTAdapter/PO/Mail")
    }

    @Test
    fun a() {
        val p = Paths.get("C:\\workspace\\2023-09-19\\tpi-integration-prod.json")
        //val p = Paths.get("C:\\workspace\\2023-09-19\\tpi-integration-test.json")
        //val p = Paths.get("C:\\workspace\\2023-09-19\\l3.all_return-shipment.json")
        val root = NiFi.parseStream(FileInputStream(p.toFile()))

        val c = root.children.find { it.name == "Kafka" }?.children?.find { it.name == "Consumer" }?.children!!
        //val c = root.children.find { it.name == "Kafka" }?.children?.find { it.name == "Producer" }?.children!!
        c.forEach { f ->
            val m = mutableMapOf<String, String>()
            f.collectvars(m, predicate)
            println(f.name)
            m.forEach { (k, v) ->
                println("\t$k = $v")
            }
        }
    }
}