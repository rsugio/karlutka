import KT.Companion.testInflux
import karlutka.util.KInflux
import karlutka.util.KTorUtils
import karlutka.util.KtorClient
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test

// https://github.com/influxdata/influxdb-client-java/tree/master/client-kotlin -
class KInfluxTests {
    val token = "XaH4I6PlO8p7HuZqCn0SsCiw4KwkXd72b_v9ETZc5e-zFHDWuKPyTgqI-Eri5v7eBTix9eZPM2iXT6fg2OMxSQ=="

    init {
        if (testInflux) {
            KtorClient.createClientEngine()
            KInflux.connect("http://localhost:8086", token.toCharArray())
            KInflux.org = "test"
            KInflux.bucket = "test1"
        }
    }

    @Test
    fun sinus() {
        if (!testInflux) return
        val from = Instant.now().toEpochMilli() - 1000
        for (i in 0..720 * 50) {
            val f = sin(i / (20 * PI)) * 10
            KInflux.addToBuffer("ns", "синус=$f", from + i)
        }
        runBlocking {
            KInflux.writeBuffer()
        }
    }


}