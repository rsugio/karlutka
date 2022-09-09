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
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

/**
 * Джонни TPZ ходок
 */
object Ztp : FileVisitor<Path> {
    lateinit var rootPath: Path
    val stack: Stack<String> = Stack()
    val root = mutableListOf<Tpz>()
    val currentTpzs = mutableListOf<TpzObject>()
    lateinit var zos: ZipOutputStream

    @Serializable
    class Tpz(
        val tpzName: String,
        val path: String,
        val objects: List<TpzObject>
    )

    @Serializable
    class TpzObject(
        val key: String,
        val typeID: String,
//        val oid: String,
//        val vid: String
    )

    fun reindex(from: Path) {
        rootPath = from
        require(Files.isDirectory(rootPath))
        val idxZip = rootPath.resolve("idx.zip")
        zos = ZipOutputStream(idxZip.outputStream())
        stack.clear()
        root.clear()
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
            currentTpzs.clear()
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
            val lst = Zatupka.meatball(file)
            lst.forEach {
                val xiobj = XiObj.decodeFromPath(it)
                val key = xiobj.key()
                val zef = ZipEntry(ze.name + key)
                zos.putNextEntry(zef)
                zos.write(it.readBytes())
                zos.closeEntry()
                Files.delete(it)
            }
            zos.flush()
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