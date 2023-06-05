import KT.Companion.x
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import ru.rsug.karlutka.pi.Scenario

@Tag("Offline")
class KScenarioOfflineTests {

    @Test
    fun parsers() {
        Scenario.decodeFromReader(x("/pi_AE_Cache/rtc01ping_req.xml"))
    }
}