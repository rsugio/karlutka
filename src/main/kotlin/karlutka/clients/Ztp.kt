package karlutka.clients

import karlutka.parsers.pi.FunctionLibrary
import karlutka.parsers.pi.XiTrafo
import karlutka.parsers.pi.Zatupka
import karlutka.util.KTempFile
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * Джонни TPZ ходок
 */
object Ztp : FileVisitor<Path> {
    lateinit var rootPath: Path
    val stack: Stack<String> = Stack()
    lateinit var zos: ZipOutputStream

    // из много TPZ один ZIP с индексами
    fun index(from: Path) {
        rootPath = from
        require(Files.isDirectory(rootPath))
        val idxZip = rootPath.resolve("idx.zip")
        zos = ZipOutputStream(idxZip.outputStream())
        stack.clear()

        Files.walkFileTree(from, this)
        require(stack.isEmpty())
        zos.close()
    }

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        if (dir != rootPath) {
            val name = dir!!.name
            stack.push(name)
            val ze = ZipEntry(stack.joinToString("/") + "/")
            zos.putNextEntry(ze)
            zos.closeEntry()
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        val name = file!!.name
        if (name.lowercase().endsWith(".tpz")) {
            println("\t$name")
            val ze = ZipEntry(stack.joinToString("/") + "/" + name + "/")
            zos.putNextEntry(ze)
            zos.closeEntry()
            Zatupka.meatball(file) {xiobj, bytearray ->
                val key = xiobj.key()
                val zef = ZipEntry(ze.name + key)
                zos.putNextEntry(zef)
                zos.write(bytearray)
                zos.closeEntry()
                zos.flush()
                // пишем контент
                val typeID = xiobj.idInfo.key.typeID
                if (typeID == "XI_TRAFO") {
                    val tr = XiTrafo.decodeFromString(xiobj.content.contentString)
                    if (tr.MetaData.blob != null) {
                        val ba = tr.MetaData.blob.content()!!
                        // запись для массового тестирования
                        try {
                            val mt = tr.toMappingTool()
//                            KTempFile.getTempFileXml("mappingtool_").writeBytes(ba)
                        } catch (e: Exception) {
                            KTempFile.getTempFileXml("error_mappingtool_").writeBytes(ba)
                            throw e
                        }
                    } else {
                        System.err.println(xiobj.key())
                    }
                } else if (typeID == "FUNC_LIB") {
                    val s = xiobj.content.contentString
                    try {
                        val fl = FunctionLibrary.decodeFromString(s)
                        val ba = fl.MetaData.blob.content()!!
                        try {
                            val fs = fl.toFunctionStorage()
                            //KTempFile.getTempFileXml("functionstorage_").writeBytes(ba)
                        } catch (e: Exception) {
                            KTempFile.getTempFileXml("error_functionstorage_").writeBytes(ba)
                            throw e
                        }
                    } catch (e: Exception) {
                        System.err.println(s)
                        throw e
                    }
                }
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        if (dir != rootPath) {
            stack.pop()
        }
        return FileVisitResult.CONTINUE
    }
}