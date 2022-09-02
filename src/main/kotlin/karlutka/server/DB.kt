package karlutka.server

import karlutka.models.MPI
import karlutka.parsers.pi.XiBasis
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

// см https://habr.com/ru/company/otus/blog/555134/ с некоторой докой
object DB {
    private const val len = 128
    private const val guidlen = 32
    lateinit var h2db: Database
    fun init(url: String) {
        h2db = Database.connect(url, "org.h2.Driver")
        println("H2 соединён на $url")
        transaction(h2db) {
            SchemaUtils.create(PI)
            SchemaUtils.create(PIAF)
            SchemaUtils.create(PIOBJ)
        }
    }

    fun close() {
        println("H2 отсоединён")
    }

    object PI : Table() {
        val sid = varchar("sid", len)
        override val primaryKey = PrimaryKey(sid)
        fun exists(s: String): Boolean {
            PI.select(sid eq s).map { return true }
            return false
        }

        fun insert(s: String) = PI.insert { it[sid] = s }
    }

    object PIAF : Table() {
        val sid = varchar("sid", len)           //TODO переделать на ссылку
        val af = varchar("af", len)
        // очевидно здесь будут ещё атрибуты
        override val primaryKey = PrimaryKey(sid, af)
    }

    object PIOBJ : Table() {
        val sid = varchar("sid", len)           //TODO переделать на ссылку
        val oid = varchar("oid", 32)
        val vid = varchar("vid", 32)
        override val primaryKey = PrimaryKey(sid, oid, vid)
        fun merge(a: List<MPI.HmiType>, t: Transaction) {

        }
    }
}