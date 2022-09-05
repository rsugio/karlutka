package karlutka.server

import io.ktor.server.plugins.compression.*
import karlutka.clients.PI
import karlutka.models.MPI
import karlutka.parsers.pi.XiBasis
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// см https://habr.com/ru/company/otus/blog/555134/ с некоторой докой
object DB {
//    private const val len = 128
    //private const val guidlen = 32
//    lateinit var h2db: Database
    lateinit var conn: Connection
    val sqls : MutableMap<String,PreparedStatement> = mutableMapOf()

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
//        conn.setClientInfo("ApplicationName", "karlutka")
        val sq = """CREATE TABLE IF NOT EXISTS PI(
  SID CHARACTER VARYING(128) not null primary key,
  store BINARY LARGE OBJECT not null
);
CREATE TABLE IF NOT EXISTS XIOBJ(
   SID CHARACTER VARYING(128) not null,
   OID CHARACTER(32) not null,
   VID CHARACTER(32) not null,
   service CHARACTER VARYING(32) not null,
   store BINARY LARGE OBJECT not null
);
"""
        conn.prepareStatement(sq).execute()
        require(conn.isValid(3))
        sqls["pisel"] = conn.prepareStatement("SELECT store FROM PI where SID=?1")
        sqls["piins"] = conn.prepareStatement("INSERT INTO PI(SID, store) VALUES(?1, ?2)")
        sqls["piupd"] = conn.prepareStatement("UPDATE PI SET store=?2 WHERE sid=?1")

        println("H2 соединён на $url")
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

    fun readStore(sql: String, arg1:String): MPI.State? {
        require(sqls.containsKey(sql))
        val ps = sqls[sql]!!
        ps.setString(1, arg1)
        if (ps.execute() && ps.resultSet.first()) {
//            val blob = ps.resultSet.getBlob("store")
            val bis = ps.resultSet.getBinaryStream("store")
            if (bis!=null) {
                val gzi = GZIPInputStream(bis).readAllBytes()
                return ProtoBuf.decodeFromByteArray<MPI.State>(gzi)
//                return ProtoBuf.decodeFromByteArray<MPI.State>(blob.getBytes(0, blob.length().toInt()))
            }
        }
        return null
    }

    fun writeStore(sql: String, arg1:String, arg2:MPI.State)  {
        require(sqls.containsKey(sql))
        val ps = sqls[sql]!!
        ps.setString(1, arg1)
        val bao = ByteArrayOutputStream()
        val gzo = GZIPOutputStream(bao)
        gzo.write(ProtoBuf.encodeToByteArray(arg2))
        gzo.close()
        bao.close()
        ps.setBlob(2, ByteArrayInputStream(bao.toByteArray()))
        require(ps.executeUpdate()==1)
    }

}