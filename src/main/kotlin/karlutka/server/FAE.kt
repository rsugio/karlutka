package karlutka.server

import karlutka.clients.PI
import java.net.URI

/**
 * Fake Adapter Engine
 */
data class FAE(
    val sid: String,
    val fakehostdb: String,
    val realHostPortURI: URI,
    val cae: PI,
) {
    val afFaHostdb = "af.$sid.$fakehostdb".lowercase()
    fun servlet() {

    }
}