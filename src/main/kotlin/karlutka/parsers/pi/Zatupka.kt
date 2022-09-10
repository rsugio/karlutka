package karlutka.parsers.pi

import karlutka.util.KTempFile
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.UnknownXmlFieldException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createFile
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

class Zatupka {
    // типы сегментов в TPT файле
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

        /**
         * //TODO написать документацию, почему так. И попробовать отревёрсить местный CRC32
         */
        fun unpage(pis: InputStream, bos: OutputStream) {
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
            pis.close()
            bos.close()
        }

        /**
         * Мапим файл на буфер, поэтому на входе не поток
         */
        @Deprecated("для памяти")
        fun list(tpt: Path): List<XiObj> {
            fun readLong(bb: ByteBuffer, pos: Long): Long {
                val b = ByteArray(8)
                bb.get(pos.toInt(), b)
                return ((b[0].toLong() shl 56) or (b[1].toLong() and 0xffL shl 48) or (b[2].toLong() and 0xffL shl 40) or (b[3].toLong() and 0xffL shl 32) or (b[4].toLong() and 0xffL shl 24) or (b[5].toLong() and 0xffL shl 16) or (b[6].toLong() and 0xffL shl 8) or (b[7].toLong() and 0xffL))
            }

            val len = Files.size(tpt)
            val raf = RandomAccessFile(tpt.toFile(), "r")
            val ch = raf.channel
            var m = ch.map(FileChannel.MapMode.READ_ONLY, 0, len)
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
            val rez = mutableListOf<XiObj>()
            catalogObj.segments.objectList.segment.filter { it.etype() == ESegmentType.modelElement }
                .forEach { segment ->
                    val slice = ByteArray(segment.length - 8)
                    m.get(segment.offset.toInt() + 8, slice)
                    try {
                        val xo = XiObj.decodeFromXmlReader(PlatformXmlReader(ByteArrayInputStream(slice), "UTF-8"))
                        rez.add(xo)
                    } catch (e: UnknownXmlFieldException) {
                        Files.createTempFile("xiobj_error_", ".xml").writeBytes(slice)
                        throw e
                    }
                }
            m = null
            ch.close()
            require(!ch.isOpen)
            raf.close()
            return rez
        }

        fun readLong(raf: RandomAccessFile, pos: Long): Long {
            val b = ByteArray(8)
            raf.seek(pos)
            raf.read(b)
            return ((b[0].toLong() shl 56) or (b[1].toLong() and 0xffL shl 48) or (b[2].toLong() and 0xffL shl 40) or (b[3].toLong() and 0xffL shl 32) or (b[4].toLong() and 0xffL shl 24) or (b[5].toLong() and 0xffL shl 16) or (b[6].toLong() and 0xffL shl 8) or (b[7].toLong() and 0xffL))
        }

        // разбирает нестраничный TPT на отдельные временные файлы с xiObj
        fun list2(tpt: Path, callback: (XiObj, ByteArray) -> Unit) {
            val len = Files.size(tpt)
            val raf = RandomAccessFile(tpt.toFile(), "r")
            val catalogBeginReal = readLong(raf, len - 8)
            val catalogBeginXml = catalogBeginReal + 8
            val catalogLengthXml = len - catalogBeginReal - 16

            val lt: Long = readLong(raf, catalogBeginReal)
            require(lt <= 0x00000000FFFFFFFFL) {
                "crc32=0x$" + java.lang.Long.toHexString(lt) + " at pos=0x" +
                        java.lang.Long.toHexString(catalogBeginReal)
            }

            val xmlcatalog = ByteArray(catalogLengthXml.toInt())
            raf.seek(catalogBeginXml); raf.read(xmlcatalog)
            val catalogObj = decodeFromString(String(xmlcatalog))

            catalogObj.segments.objectList.segment
                .filter { it.etype() == ESegmentType.modelElement }
                .forEach { segment ->
                    // для обычных сегментов первые 8 байт непонятные, в случае XML неважны
                    // так как мы берём данные из XML-каталога неизменными, то читаем здесь со смещением
                    val slice = ByteArray(segment.length - 8)
                    raf.seek(segment.offset + 8L); raf.read(slice)
                    try {
                        // Читаем и парсим XML чтобы проверить структуру, но возвращаем назад всё равно зип
                        val xo = XiObj.decodeFromXmlReader(PlatformXmlReader(ByteArrayInputStream(slice), "UTF-8"))
                        callback.invoke(xo, slice)
                    } catch (e: UnknownXmlFieldException) {
                        // подумать как возвращать ошибки. Пока для диагностики так.
                        Files.createTempFile("xiobj_error_", ".xml").writeBytes(slice)
                        throw e
                    }
                }
            raf.close()
        }

        // полный фарш - пример распаковки, скорее здесь для демо
        fun meatball(srctpz: Path, callback: (XiObj, ByteArray) -> Unit) : String? {
            val zip = ZipFile(srctpz.toFile())
            val e = zip.entries().toList().find { it.name.lowercase().endsWith(".tpt") && !it.isDirectory }
            if (e==null) return null
            val tpt = KTempFile.getTempFileTpt()
            unpage(zip.getInputStream(e), tpt.outputStream())
            list2(tpt, callback)
            Files.delete(tpt)
            return e.name
/*
            val w = KTempFile.getTempFileXiObj()
            w.writeBytes(slice)
            rez.add(w)
 */
        }
    }
}