import KT.Companion.s
import KT.Companion.x
import karlutka.parsers.pi.SimpleQuery
import karlutka.server.SPROXY
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@Suppress("UNUSED_VALUE")
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
        SPROXY.decodeFromString(objects.encodeToString())
        SPROXY.decodeFromString(s("/pi_ESR/objects.json"))
    }

    @Test
    fun sproxy() {
        // последовательность вызовов, как она приходит из SPROXY
        SimpleQuery.decodeFromReaderRequest(x("/pi_ESR/esr01request.xml"))
        SimpleQuery.decodeFromReaderResult(x("/pi_ESR/esr02result.xml"))
        SimpleQuery.decodeFromReaderRequest(x("/pi_ESR/esr03request.xml"))
        SimpleQuery.decodeFromReaderResult(x("/pi_ESR/esr04result.xml"))
        SimpleQuery.decodeFromReaderRequest(x("/pi_ESR/esr05ValueMappingReplicationOut.xml"))
        SimpleQuery.decodeFromReaderResult(x("/pi_ESR/esr06ValueMappingReplicationOut.xml"))
    }

}