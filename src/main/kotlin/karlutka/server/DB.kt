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
    val sq: MutableMap<String, PreparedStatement> = mutableMapOf()

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
        val sq = """CREATE TABLE IF NOT EXISTS PI(
  SID CHARACTER VARYING(32) not null primary key,
  store BINARY LARGE OBJECT not null
);
CREATE TABLE IF NOT EXISTS XIOBJ(
  foreign key (sid) references PI(SID), 
  SID CHARACTER VARYING(128) not null,
  OID CHARACTER(32) not null,
  VID CHARACTER(32) not null,
  serv CHARACTER VARYING(32) not null,
  obj CHARACTER VARYING(655360),
  primary key (SID,OID,VID,serv)
);
"""
        conn.prepareStatement(sq).execute()
        require(conn.isValid(3))
        this.sq["pisel"] = conn.prepareStatement("SELECT store FROM PI where SID=?1")
        this.sq["piins"] = conn.prepareStatement("INSERT INTO PI(SID, store) VALUES(?1, ?2)")
        this.sq["piupd"] = conn.prepareStatement("UPDATE PI SET store=?2 WHERE sid=?1")

        this.sq["xi1"] = conn.prepareStatement("SELECT obj FROM XIOBJ where SID=?1 and OID=?2 and VID=?3 and serv=?4")
        this.sq["xiv"] = conn.prepareStatement("SELECT VID,serv,obj FROM XIOBJ where SID=?1 and OID=?2")
        this.sq["xim"] = conn.prepareStatement("INSERT INTO XIOBJ (SID,OID,VID,serv,obj) VALUES (?1,?2,?3,?4,?5)")
        this.sq["xi2"] = conn.prepareStatement("SELECT OID,VID FROM XIOBJ where SID=?1")

        println("H2 соединён на $url")
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

    fun readStore(sql: String, arg1: String): MPI.State? {
        require(sq.containsKey(sql))
        val ps = sq[sql]!!
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
        require(sq.containsKey(sql))
        val ps = sq[sql]!!
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