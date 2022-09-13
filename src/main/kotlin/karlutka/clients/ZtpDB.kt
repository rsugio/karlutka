package karlutka.clients

import karlutka.models.MPI
import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.Zatupka
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement.RETURN_GENERATED_KEYS
import java.util.*
import kotlin.io.path.name

/**
 * Джонни TPZ ходок
 */
object ZtpDB : FileVisitor<Path> {
    lateinit var rootPath: Path
    lateinit var conn: Connection
    val stack: Stack<String> = Stack()
    val swcv = mutableMapOf<String, String>()
    val esrobjects = mutableListOf<MPI.EsrObj>()

    const val sqInit = """
CREATE TABLE IF NOT EXISTS PUBLIC.SWCV(
  GUID   CHAR(32) not null primary key,
  CAPTION VARCHAR(128) not null
);

CREATE TABLE IF NOT EXISTS PUBLIC.TPZ(
  NUM    INT auto_increment not null primary key,
  PATH   VARCHAR(128) not null unique 
);
        
CREATE TABLE IF NOT EXISTS PUBLIC.ESROBJ(
  NUM    INT auto_increment not null primary key,
  TYPEID VARCHAR(16) not null,
  OID    CHAR(32) not null,
  SWCVID CHAR(32) references PUBLIC.SWCV(GUID),
  SWCVSP TINYINT NOT NULL,                    // -128 to 127 
  KEY_   VARCHAR(256) not null,               // имя через палки
  constraint EU unique(typeid, oid, swcvid, key_)
);

CREATE TABLE IF NOT EXISTS PUBLIC.ESRVER(    // ESR OBJECT VERSIONS
  NUM    INT auto_increment not null primary key,
  TPZNUM INT references PUBLIC.TPZ(NUM),
  OBJNUM INT references PUBLIC.ESROBJ(NUM),
  VID    CHAR(32) not null,
  TEXT   VARCHAR(256)                         // текстовое описание
);

CREATE TABLE IF NOT EXISTS PUBLIC.ESRVL(      //ESR VERSION LINK
  VERNUM INT references PUBLIC.ESRVER(NUM),
  ROLE   VARCHAR(32) not null,                // .
  KPOS   INT not null,
  OBJNUM INT references PUBLIC.ESROBJ(NUM)
);
"""
    lateinit var swcvIns: PreparedStatement
    lateinit var tpzIns: PreparedStatement
    lateinit var esrobjIns: PreparedStatement

    lateinit var esrverIns: PreparedStatement
    lateinit var esrlinkIns: PreparedStatement

    fun index(from: Path) {
        rootPath = from
        require(Files.isDirectory(from))

        conn = DriverManager.getConnection("jdbc:h2:$from/h2")
        conn.prepareStatement(sqInit).execute()
        swcvIns = conn.prepareStatement("INSERT INTO PUBLIC.SWCV(GUID,CAPTION) VALUES(?1,?2)")
        tpzIns = conn.prepareStatement("INSERT INTO PUBLIC.TPZ(PATH) VALUES(?1)", RETURN_GENERATED_KEYS)
        esrobjIns = conn.prepareStatement(
            "INSERT INTO PUBLIC.ESROBJ(TYPEID,OID,SWCVID,SWCVSP,KEY_) VALUES(?1,?2,?3,?4,?5)", RETURN_GENERATED_KEYS
        )
        esrverIns = conn.prepareStatement(
            "INSERT INTO PUBLIC.ESRVER(TPZNUM,OBJNUM,VID,TEXT) VALUES(?1,?2,?3,?4)", RETURN_GENERATED_KEYS
        )
        esrlinkIns = conn.prepareStatement("INSERT INTO PUBLIC.ESRVL(VERNUM,ROLE,KPOS,OBJNUM) VALUES(?1,?2,?3,?4)")

        stack.clear()
        Files.walkFileTree(from, this)
        require(stack.isEmpty())
    }

    fun oneXiObj(tpznum: Int, xiobj: XiObj) {
        val text = xiobj.text()
        val esrobj = xiobj.esrobject()
        val vid = xiobj.idInfo.VID!!

        if (!swcv.containsKey(esrobj.swcvid)) {
            val swcvcaption = xiobj.idInfo.vc!!.caption!!
            swcv[esrobj.swcvid] = swcvcaption
            swcvIns.setString(1, esrobj.swcvid)
            swcvIns.setString(2, swcvcaption)
            require(swcvIns.executeUpdate() == 1)
        }
        // Перечень ESR-объектов
        val esr2 = esrobjects.find { it == esrobj }
        if (esr2 == null) {
            esrobjIns.setString(1, esrobj.typeID.toString())
            esrobjIns.setString(2, esrobj.oid)
            esrobjIns.setString(3, esrobj.swcvid)
            esrobjIns.setInt(4, esrobj.swcvsp)
            esrobjIns.setString(5, esrobj.key)
            require(esrobjIns.executeUpdate() == 1)
            require(esrobjIns.generatedKeys.next())
            esrobj.num = esrobjIns.generatedKeys.getInt(1)
            esrobjects.add(esrobj)
        } else {
            esrobj.num = esr2.num
        }
        // пишем версию
        esrverIns.setInt(1, tpznum)
        esrverIns.setInt(2, esrobj.num)
        esrverIns.setString(3, vid)
        esrverIns.setString(4, text)
        require(esrverIns.executeUpdate() == 1)
        require(esrverIns.generatedKeys.next())
        val vernum = esrverIns.generatedKeys.getInt(1)
        val roles = xiobj.generic.lnks?.x?.filter { it.role != "_inner" && it.role != "_original" }
        val _inner = xiobj.generic.lnks?.x?.filter { it.role == "_inner"}
        val _original = xiobj.generic.lnks?.x?.filter { it.role == "_original"}
        roles?.forEach { lnk: XiObj.Generic.LnkRole ->
            val linked = lnk.lnk.esrobject(esrobj)
            // есть ли такой swcv в списке?
            if (!swcv.containsKey(linked.swcvid)) {
                if (lnk.lnk.vc == null || lnk.lnk.vc.caption == null) {
                    error("Ссылочный объект корявый: у $esrobj (role=${lnk.role})")
                }
                val swcvcaption = lnk.lnk.vc.caption
                swcv[linked.swcvid] = swcvcaption
                swcvIns.setString(1, linked.swcvid)
                swcvIns.setString(2, swcvcaption)
                require(swcvIns.executeUpdate() == 1)
            }
            // есть ли ссылочный объект в списке?
            val linkedesr2 = esrobjects.find { it == linked }
            if (linkedesr2 == null) {
                esrobjIns.setString(1, linked.typeID.toString())
                esrobjIns.setString(2, linked.oid)
                esrobjIns.setString(3, linked.swcvid)
                esrobjIns.setInt(4, linked.swcvsp)
                esrobjIns.setString(5, linked.key)
                require(esrobjIns.executeUpdate() == 1)
                require(esrobjIns.generatedKeys.next())
                linked.num = esrobjIns.generatedKeys.getInt(1)
                esrobjects.add(linked)
            } else {
                linked.num = linkedesr2.num
            }
            esrlinkIns.setInt(1, vernum)
            esrlinkIns.setString(2, lnk.role)
            esrlinkIns.setInt(3, lnk.kpos)
            esrlinkIns.setInt(4, linked.num)
            require(esrlinkIns.executeUpdate() == 1)
        } // цикл по ссылкам-ролям
    }

    fun visitTpz(file: Path) {
        val path = stack.joinToString("/") + "/${file.name}"
        println(path)
        conn.beginRequest()

        tpzIns.setString(1, path)
        require(tpzIns.executeUpdate() == 1)
        val rs = tpzIns.generatedKeys
        require(rs.next())
        val tpznum = rs.getInt(1)
        Zatupka.meatball(file) { xiobj, bytearray ->
            val banned = listOf(
                "process", "processstep", "ariscommonfile", "arisfilter", "arisfssheet", "arisreport", "aristemplate"
            ).contains(xiobj.idInfo.key.typeID)
            if (!banned) {
                oneXiObj(tpznum, xiobj)
            }
        }
        conn.endRequest()
        conn.commit()
    }

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        stack.push(dir!!.name)
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        if (file!!.name.lowercase().endsWith(".tpz")) {
            visitTpz(file)
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