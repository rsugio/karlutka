package karlutka.models

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }

    data class Swcv(
        val id: String,         //гуид
        val vendor: String,
        val name: String,
        val version: String,
        val type: Char,         //S, L, ?
        val language: String,
        val ws_name: String,    // SC_I_END, 1.0 of vendor.com
    )
}