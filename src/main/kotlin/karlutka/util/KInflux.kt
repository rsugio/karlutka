package karlutka.util

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

object KInflux {
    lateinit var client: HttpClient
    var org: String = ""
    var bucket: String = ""
    val buffer = StringBuffer(16 * 1024 * 1024)

    fun init(kf: KfInfluxDB, auths: MutableList<KfAuth>): String {
        val a = auths.find { it.id == kf.auth }
        require(a is KfAuth.ApiToken)
        val s = connect(kf.host, a.token)
        org = kf.org
        bucket = kf.bucket
        return s
    }

    fun connect(host: String, token: CharArray): String {
        client = HttpClient(KtorClient.clientEngine) {
            expectSuccess = true
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()
            }
            defaultRequest {
                url(host)
                header("Authorization", "Token ${String(token)}")
            }
        }
        var s: String
        runBlocking {
            val resp = client.post("/api/v2")
            require(resp.status.isSuccess() && resp.contentType()!!.match("application/json"))
            s = client.get("/health").bodyAsText().trim()
            require(s.contains("influxdb"))
        }
        return s
    }

    fun addToBuffer(measurementTags: String, fields: String, timestampMS: Long) {
        buffer.append(measurementTags).append(' ').append(fields).append(' ').append(timestampMS).append('\n')
    }

    fun addToBuffer(rawLines: String) {
        // см https://docs.influxdata.com/influxdb/cloud/reference/syntax/line-protocol/
        buffer.append(rawLines).append('\n')
    }

    suspend fun writeBuffer() {
        // https://docs.influxdata.com/influxdb/v2.3/write-data/best-practices/optimize-writes/
        // The optimal batch size is 5000 lines of line protocol.
        val s = buffer.toString()
        buffer.setLength(0)
        write(s)
    }

    suspend fun write(s: String) {
//        Paths.get("C:\\workspace\\rsug.io\\karlutka2\\karlutka2\\src\\test\\resources\\influx\\sinus.txt").writeText(s)
        val resp = client.post("/api/v2/write?org=$org&bucket=$bucket&precision=ms") {
            header("Content-Type", "text/plain; charset=utf-8")
            setBody(s)
        }
        require(resp.status.isSuccess() && resp.bodyAsText() == "")
        println("write OK")
    }

    fun close() {
        client.close()
    }

}