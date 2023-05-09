import KT.Companion.s
import KT.Companion.x
import karlutka.parsers.pi.SimpleQuery
import karlutka.parsers.pi.XiObj
import karlutka.server.SPROXY
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Paths

//@Suppress("UNUSED_VALUE")
class KSimpleQueryTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            SPROXY.load(Paths.get("c:/data/tmp/ESR"))
        }
    }

    @Test
    fun a() {
        val a = "_"
        val wksp = SPROXY.Workspace(a, a, a, a, a, a, 'A', a, a)
        val objects = SPROXY.Objects(wksp, mutableMapOf("ns00" to "ns00", "ns01" to "ns01"))
        objects.encodeToString()
        SPROXY.decodeObjectsFromString(objects.encodeToString())
        SPROXY.decodeObjectsFromString(s("/pi_ESR/objects.json"))
        val n = SimpleQuery.decodeNavigationFromReader(x("/pi_ESR/navirequest01.xml"))
        println(n)
    }


    @Test
    fun sproxy() {
        // последовательность вызовов, как она приходит из SPROXY
        var s: String
        s = SPROXY.handle(s("/pi_ESR/esr01request.xml"))
        SimpleQuery.decodeResultFromReader(x("/pi_ESR/esr02result.xml"))
        SimpleQuery.decodeRequestFromReader(x("/pi_ESR/esr03request.xml"))
        SimpleQuery.decodeResultFromReader(x("/pi_ESR/esr04result.xml"))
        SimpleQuery.decodeRequestFromReader(x("/pi_ESR/esr05ValueMappingReplicationOut.xml"))
        SimpleQuery.decodeResultFromReader(x("/pi_ESR/esr06ValueMappingReplicationOut.xml"))
        s = SPROXY.handle(s("/pi_ESR/esr05ValueMappingReplicationOut.xml"))
        s = SPROXY.handle(s("/pi_ESR/esr07namespaces.xml"))
        //println(s)
        SPROXY.navigation(s("/pi_ESR/navirequest01.xml"))

    }

    @Test
    fun xiObj() {
        val x = XiObj.decodeFromXmlReader(x("/pi_ESR/ValueMappingReplicationOut.ifmmessif"))
        println(x)
    }

}