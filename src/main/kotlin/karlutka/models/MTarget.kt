package karlutka.models

import karlutka.util.KfTarget

interface MTarget {
    val konfig: KfTarget
    fun getSid() = konfig.sid
}