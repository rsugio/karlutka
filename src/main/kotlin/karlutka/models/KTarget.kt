package karlutka.models

import karlutka.util.KfTarget

interface KTarget {
    val konfig: KfTarget
    fun getSid() = konfig.sid
}