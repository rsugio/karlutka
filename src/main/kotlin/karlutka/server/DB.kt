package karlutka.server

import karlutka.models.MPI
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// см https://habr.com/ru/company/otus/blog/555134/ с некоторой докой
object DB {
    lateinit var conn: Connection
    val sqls: MutableMap<String, PreparedStatement> = mutableMapOf()

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
        val sq = """CREATE TABLE IF NOT EXISTS PI(
  SID CHARACTER VARYING(128) not null primary key,
  store BINARY LARGE OBJECT not null
);
CREATE TABLE IF NOT EXISTS XIOBJ(
  SID CHARACTER VARYING(128) not null,
  OID CHARACTER(32) not null,
  VID CHARACTER(32) not null,
  serv CHARACTER VARYING(32) not null,
  store BINARY LARGE OBJECT not null,
  primary key (SID,OID,VID,serv)
);
"""
        conn.prepareStatement(sq).execute()
        require(conn.isValid(3))
        sqls["pisel"] = conn.prepareStatement("SELECT store FROM PI where SID=?1")
        sqls["piins"] = conn.prepareStatement("INSERT INTO PI(SID, store) VALUES(?1, ?2)")
        sqls["piupd"] = conn.prepareStatement("UPDATE PI SET store=?2 WHERE sid=?1")

        sqls["xi1"] = conn.prepareStatement("SELECT store FROM XIOBJ where SID=?1 and OID=?2 and VID=?3 and serv=?4")
        sqls["xiv"] = conn.prepareStatement("SELECT VID,serv,store FROM XIOBJ where SID=?1 and OID=?2")
        sqls["xim"] = conn.prepareStatement("INSERT INTO XIOBJ (SID,OID,VID,serv,store) VALUES (?1,?2,?3,?4,?5)")

        println("H2 соединён на $url")
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

    fun readStore(sql: String, arg1: String): MPI.State? {
        require(sqls.containsKey(sql))
        val ps = sqls[sql]!!
        ps.setString(1, arg1)
        if (ps.execute() && ps.resultSet.first()) {
            val bis = ps.resultSet.getBinaryStream("store")
            if (bis != null) {
                return ProtoBuf.decodeFromByteArray<MPI.State>(GZIPInputStream(bis).readAllBytes())
            }
        }
        return null
    }

    fun writeStore(sql: String, arg1: String, arg2: MPI.State) {
        require(sqls.containsKey(sql))
        val ps = sqls[sql]!!
        ps.setString(1, arg1)
        val bao = ByteArrayOutputStream()
        val gzo = GZIPOutputStream(bao)
        gzo.write(ProtoBuf.encodeToByteArray(arg2))
        gzo.close()
        bao.close()
        ps.setBlob(2, ByteArrayInputStream(bao.toByteArray()))
        require(ps.executeUpdate() == 1)
    }
}