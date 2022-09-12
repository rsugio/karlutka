package karlutka.clients

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
    val sq: MutableMap<String, PreparedStatement> = mutableMapOf()

    const val sqInit = """
CREATE TABLE IF NOT EXISTS PUBLIC.TPZ(
  NUM INT4 auto_increment not null primary key,
  PATH varchar(128) not null unique 
);
        
CREATE TABLE IF NOT EXISTS PUBLIC.ESROBJ(
  TPZNUM INT4 REFERENCES TPZ(NUM),
  TYPEID CHAR VARYING(16) not null,
  OID CHAR(32) not null,
  SWCVID CHAR(32) not null,
  SWCVSP TINYINT NOT NULL,                  // -128 to 127 
  KEY_ CHARACTER VARYING(256) not null,     // имя через палки
  TEXT CHAR VARYING(128),                   // текстовое описание, пока только на языке EN или первое попавшееся
  primary key (TPZNUM,TYPEID,OID)
);
"""
    fun index(from: Path) {
        rootPath = from
        require(Files.isDirectory(from))
        conn = DriverManager.getConnection("jdbc:h2:$from/h2")
        conn.prepareStatement(sqInit).execute()
//        this.sq["pisel"] = conn.prepareStatement("SELECT store FROM PI where SID=?1")
        this.sq["tpzins"] = conn.prepareStatement("INSERT INTO PUBLIC.TPZ(PATH) VALUES(?1)", RETURN_GENERATED_KEYS)
        this.sq["eoins"] = conn.prepareStatement("INSERT INTO PUBLIC.ESROBJ(TPZNUM,TYPEID,OID,SWCVID,SWCVSP,KEY_,TEXT) VALUES(?1,?2,?3,?4,?5,?6,?7)")
//        this.sq["piupd"] = conn.prepareStatement("UPDATE PI SET store=?2 WHERE sid=?1")
        stack.clear()
        Files.walkFileTree(from, this)
        require(stack.isEmpty())
    }

    fun visitTpz(file: Path) {
        val path = stack.joinToString("/")+"/${file.name}"
        println(path)
        val p = sq["tpzins"]!!
        p.setString(1, path)
        require(p.executeUpdate()==1)
        val rs = p.generatedKeys
        require(rs.next())
        val tpznum = rs.getInt(1)
        val ps = sq["eoins"]!!
        Zatupka.meatball(file) {xiobj, bytearray ->
            // пишем контент
            val typeID = xiobj.idInfo.key.typeID
            val oid = xiobj.idInfo.key.oid!!
            val swcvid = xiobj.idInfo.vc!!.swcGuid!!
            require(swcvid.length==32)
            val swcvsp = xiobj.idInfo.vc.sp!!
            require(swcvsp==-1 || swcvsp>0)
            val key = xiobj.idInfo.key.elem
            ps.setInt(1, tpznum)
            ps.setString(2, typeID)
            ps.setString(3, oid)
            ps.setString(4, swcvid)
            ps.setInt(5, swcvsp)
            ps.setString(6, key.joinToString("|"))
            ps.setString(7, "")
            require(ps.executeUpdate()==1)
        }

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