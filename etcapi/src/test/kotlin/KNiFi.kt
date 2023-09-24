import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import ru.rsug.karlutka.nifi.NiFi
import java.nio.file.Paths
import java.util.function.Predicate
import kotlin.io.path.readText

class KNiFi {

    private fun printProcess(depth: Int, c: NiFi.NFlowContents, parent: NiFi.TL? = null): NiFi.TL {
        val tl = NiFi.TL(c.name, c.variables)
        parent?.children?.add(tl)
        c.processGroups.forEach { t ->
            printProcess(depth + 1, t, tl)
        }
        return tl
    }

    private val predicate = { x: Map.Entry<String, String> ->
        val banned = listOf("", "PO.Password", "S3.SecretKey", "Email.From", "Email.To", "Email.Subject", "mime.type")
        !banned.contains(x.key) && !x.value.endsWith("/RESTAdapter/PO/Mail")
    }

    @Test
    fun a() {
        val p = Paths.get("C:\\workspace\\2023-09-19\\tpi-integration-prod.json")
        //val p = Paths.get("C:\\workspace\\2023-09-19\\tpi-integration-test.json")
        //val p = Paths.get("C:\\workspace\\2023-09-19\\l3.all_return-shipment.json")
        val d = Json.decodeFromString<NiFi.NFlowDefinition>(p.readText())
        val root = NiFi.TL("root")
        d.flowContents.processGroups.forEach { t ->
            printProcess(0, t, root)
        }
        val c = root.children.find { it.name == "Kafka" }?.children?.find { it.name == "Consumer" }?.children!!
        //val c = root.children.find { it.name == "Kafka" }?.children?.find { it.name == "Producer" }?.children!!
        c.forEach { f ->
            val m = mutableMapOf<String, String>()
            f.collectvars(m, predicate)
            //println(f)
            println(f.name)
            m.forEach { (k, v) ->
                println("\t$k = $v")
            }
        }
    }
}