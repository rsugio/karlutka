package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class Zatupka {
    enum class ESegmentType {
        header,             //XML заголовок выгрузки - ничего интересного
        metaModelPVC,       //XML с непонятной схемой
        metaModelModel,     //XML со ссылкой, из каких областей была выгрузка, умеренно интересно
        pvcVersionSet,      //двоичный экспорт лист
        pvcVersionSetSpecialization,    //двоичный мелкий
        modelVersionSet,    //пусто
        modelGeneralData,   //XML с метаданными выгрузки, возможно интересен
        pvcElement,         //двоичный почти одинакового размера пакет (384 или 386)
        modelElement,       //XML xiObj - самое ценное
        knownDevlines       //двоичный список SWCV (порядок зависимостей?)
    }

    @Serializable
    @XmlSerialName("catalog", "", "")
    class Catalog(
        @XmlElement(true) val segments: Segments
    )

    @Serializable
    @XmlSerialName("segments", "", "")
    class Segments(
        @XmlElement(true) val segment: List<Segment>, @XmlElement(true) val objectList: ObjectList
    )

    @Serializable
    @XmlSerialName("segment", "", "")
    class Segment(
        val type: String, val offset: Int, val length: Int
    ) {
        fun etype() = ESegmentType.valueOf(type.replace("-", ""))

        init {
            etype()
        }
    }

    @Serializable
    @XmlSerialName("objectList", "", "")
    class ObjectList(
        @XmlElement(true) val segment: List<Segment>
    )

    companion object {
        fun decodeFromString(sxml: String) = XML.decodeFromString<Catalog>(sxml)
        fun unpage(psrc: Path, rez: Path) {
            val pis = psrc.inputStream()
            val bos = rez.outputStream()
            val pageSize = 0x100000
            var actualRead = pageSize
            var crcRead = 0
            val pageBytes = ByteArray(pageSize)
            val b8 = ByteArray(8) { 0 }
            while (actualRead == pageSize) {
                val z8 = b8[0].toInt() == 0 && b8[1].toInt() == 0 && b8[2].toInt() == 0 && b8[3].toInt() == 0
                var x = 1
                actualRead = pis.read(pageBytes)
                while (actualRead < pageSize && x > 0) {
                    x = pis.read(pageBytes, actualRead, pageSize - actualRead)
                    if (x > 0) {
                        actualRead += x
                    }
                }
                if (crcRead == 8 && !z8) {
                    bos.write(b8, 0, crcRead)
                    bos.write(pageBytes, 8, actualRead - 8)
                } else {
                    bos.write(pageBytes, 0, actualRead)
                }
                crcRead = pis.read(b8)
            }
            bos.close()
        }

        // по байтовому смещению читает лонг
        fun readLong(bb: ByteBuffer, pos: Long): Long {
            val b = ByteArray(8)
            bb.get(pos.toInt(), b)
            val l = ((b[0].toLong() shl 56) or (b[1].toLong() and 0xffL shl 48) or (b[2].toLong() and 0xffL shl 40)
                    or (b[3].toLong() and 0xffL shl 32) or (b[4].toLong() and 0xffL shl 24)
                    or (b[5].toLong() and 0xffL shl 16) or (b[6].toLong() and 0xffL shl 8) or (b[7].toLong() and 0xffL))
            return l
        }

        fun list(tpt: Path) {
            val len = Files.size(tpt)
            val m = RandomAccessFile(tpt.toFile(), "r").channel.map(FileChannel.MapMode.READ_ONLY, 0, len)
            val catalogBeginReal = readLong(m, len - 8)
            val catalogBeginXml = catalogBeginReal + 8
            val catalogLengthXml = len - catalogBeginReal - 16

            val lt: Long = readLong(m, catalogBeginReal)
            require(lt <= 0xFFFFFFFFL) {
                "crc32=0x" + java.lang.Long.toHexString(lt) + " at pos=0x" + java.lang.Long.toHexString(
                    catalogBeginReal
                )
            }

            val xmlcatalog = ByteArray(catalogLengthXml.toInt())
            m.get(catalogBeginXml.toInt(), xmlcatalog)
            val catalogObj = decodeFromString(String(xmlcatalog))
            catalogObj.segments.objectList.segment
                .filter { it.etype() == ESegmentType.modelElement }
                .forEach { segment ->
                    val slice = ByteArray(segment.length - 8)
                    m.get(segment.offset + 8, slice)
                    val xo = XiObj.decodeFromXmlReader(PlatformXmlReader(ByteArrayInputStream(slice), "UTF-8"))
                    println(xo.idInfo.key.typeID)
                }
//            val tl = mutableListOf<Segment>()
//            tl.addAll(catalogObj.segments.segment)
//            tl.addAll(catalogObj.segments.objectList.segment)
//            tl.forEachIndexed { ix, segment ->
//                val slice = ByteArray(segment.length - 8)
//                m.get(segment.offset + 8, slice)
//                Paths.get("c:\\data\\tmp\\tpz_${ix}_${segment.etype()}.bin").writeBytes(slice)
//            }
        }
    }
}