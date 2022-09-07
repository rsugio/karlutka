package karlutka.parsers.pi

class Zatupka {
    var records = mutableListOf<CatRec>()

    data class CatRec (
        val type: String,
        val idx: Int,
        val offset: Int,
        val length: Int,
        var beginData: Int = 0,
        var lengthData: Int = 0
    ) {
//        isXml =
//        t == "header" || t == "metaModel-PVC" || t == "modelGeneralData" || t == "modelElement" || t == "metaModel-Model"
//        isBin =
//        t == "modelVersionSet" || t == "pvcVersionSet" || t == "pvcVersionSetSpecialization" || t == "knownDevlines" || t == "pvcElement"
//        assert(isXml || isBin) { "Unknown type $t" }
    }

    companion object {
        fun CatRec(index: Int, t: String, o: String, l: String): CatRec {
            return CatRec(t, index, o.toInt(), l.toInt())
        }
    }
}