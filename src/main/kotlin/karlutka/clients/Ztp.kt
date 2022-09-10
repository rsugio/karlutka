package karlutka.clients

import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.Zatupka
import kotlinx.serialization.Serializable
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

/**
 * Джонни TPZ ходок
 */
object Ztp : FileVisitor<Path> {
    lateinit var rootPath: Path
    val stack: Stack<String> = Stack()
    lateinit var zos: ZipOutputStream

    // из одного TPZ один ZIP
    fun reindexTpzsToZips(from: Path) {
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
            println(ze.name)
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
//            val lst = Zatupka.meatball(file)
//            lst.forEach {
//                val xiobj = XiObj.decodeFromPath(it)
//                val key = xiobj.key()
//                val zef = ZipEntry(ze.name + key)
//                zos.putNextEntry(zef)
//                zos.write(it.readBytes())
//                zos.closeEntry()
//                Files.delete(it)
//            }


        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        TODO("Not yet implemented")
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        if (dir != rootPath) {
            stack.pop()
        }
        return FileVisitResult.CONTINUE
    }
}