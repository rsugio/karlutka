package karlutka.clients

import karlutka.parsers.pi.Zatupka
import kotlinx.serialization.Serializable
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.io.path.name

/**
 * Джонни TPZ ходок
 */
object Ztp : FileVisitor<Path> {
    val stack: Stack<String> = Stack()
    val root = mutableListOf<Tpz>()
    val currentTpzs = mutableListOf<TpzObject>()

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
        stack.clear()
        root.clear()
        Files.walkFileTree(from, this)
        require(stack.isEmpty())
    }

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        stack.push(dir!!.name)
        currentTpzs.clear()
        println(stack)
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        val name = file!!.name
        if (name.lowercase().endsWith(".tpz")) {
            println("\t$name")
            val lst = Zatupka.meatball(file)
            println(lst)
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        TODO("Not yet implemented")
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        stack.pop()


        return FileVisitResult.CONTINUE
    }
}