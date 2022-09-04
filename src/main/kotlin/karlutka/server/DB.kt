package karlutka.server

import karlutka.clients.PI
import karlutka.models.MPI
import karlutka.parsers.pi.XiBasis
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

// см https://habr.com/ru/company/otus/blog/555134/ с некоторой докой
object DB {
//    private const val len = 128
    //private const val guidlen = 32
//    lateinit var h2db: Database
    lateinit var conn: Connection
    lateinit var sql01 : PreparedStatement

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
        sql01 = conn.prepareStatement("CREATE TABLE customers()")
        require(conn.isValid(3))

//        h2db = Database.connect(url, "org.h2.Driver")
        println("H2 соединён на $url")
//        transaction(h2db) {
//            SchemaUtils.create(PI)
//        }
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

//    object PI : Table() {
//        val sid = varchar("sid", 128)
//        val store = blob("store").nullable()
//        override val primaryKey = PrimaryKey(sid)
//        fun exists(s: String): Boolean {
//            PI.select(sid eq s).map { return true }
//            return false
//        }
//        fun insert(s: String) = PI.insert { it[sid] = s }
//        fun update1(s: String, state: MPI.State) {
//            val bt = ProtoBuf.encodeToByteArray(state)
//
//        }
//    }

}