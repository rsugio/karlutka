package karlutka.clients

import karlutka.models.MPI
import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.Zatupka
import karlutka.server.DB
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.Connection
import java.util.*
import kotlin.io.path.name

/**
 * Джонни TPZ ходок
 */
object ZtpDB : FileVisitor<Path> {
    lateinit var rootPath: Path
    lateinit var conn: Connection
    val stack: Stack<String> = Stack()

    fun index(from: Path) {
        rootPath = from
        require(Files.isDirectory(from))
        stack.clear()
        Files.walkFileTree(from, this)
        require(stack.isEmpty())
    }

    fun oneXiObj(tpznum: Int, xiobj: XiObj) {
        val text = xiobj.text()
        var esrobj = xiobj.esrobject()
        val vid = xiobj.idInfo.VID!!

        var swcv = DB.swcv.find { it.guid == esrobj.swcvid }
        if (swcv == null) {
            swcv = MPI.Swcv(esrobj.swcvid, xiobj.idInfo.vc!!.caption!!, null, null, null, null)
            DB.writeSwcv(swcv)
        }

        // Перечень ESR-объектов
        val esr2 = DB.esrobjects.find { it == esrobj }
        if (esr2 == null) {
            DB.writeEsrObj(esrobj)
        } else {
            esrobj = esr2
        }
        // попали сюда - ранее TPZ не было
        val vernum = DB.writeEsrObjVersion(esrobj, tpznum, vid, text)
        val roles = xiobj.generic.lnks?.x?.filter { it.role != "_inner" && it.role != "_original" }
        val _inner = xiobj.generic.lnks?.x?.filter { it.role == "_inner" }
        val _original = xiobj.generic.lnks?.x?.filter { it.role == "_original" }
        roles?.forEach { lnk: XiObj.Generic.LnkRole ->
            var linked = lnk.lnk.esrobject(esrobj)
            var swcvlinked = DB.swcv.find { it.guid == linked.swcvid }
            // есть ли такой swcv в списке?
            if (swcvlinked==null) {
                if (lnk.lnk.vc == null || lnk.lnk.vc.caption == null) {
                    error("Ссылочный объект корявый: у $esrobj (role=${lnk.role})")
                }
                val swcvcaption = lnk.lnk.vc.caption
                swcvlinked = MPI.Swcv(linked.swcvid, swcvcaption, null, null, null, null)
                DB.writeSwcv(swcvlinked)
            }
            // есть ли ссылочный объект в списке?
            val linkedesr2 = DB.esrobjects.find { it == linked }
            if (linkedesr2 == null) {
                DB.writeEsrObj(linked)
            } else {
                linked = linkedesr2
            }
            DB.writeEsrObjLink(vernum, lnk.role, lnk.kpos, linked)
        } // цикл по ссылкам-ролям
    }

    fun visitTpz(file: Path, tpznum: Int) {
        println(file.toString())
        Zatupka.meatball(file) { xiobj, bytearray ->
            val banned = listOf(
                "process", "processstep",
                "ariscommonfile", "arisfilter", "arisfssheet", "arisreport", "aristemplate",
                "RepBProcess"
            ).contains(xiobj.idInfo.key.typeID)
            if (!banned) {
                oneXiObj(tpznum, xiobj)
            }
        }
    }

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        stack.push(dir!!.name)
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        if (file!!.name.lowercase().endsWith(".tpz")) {
            val num = DB.getTpzNumber(file) { num ->
                visitTpz(file, num)
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        stack.pop()
        return FileVisitResult.CONTINUE
    }
}