package karlutka.server

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

// см https://habr.com/ru/company/otus/blog/555134/ с некоторой докой
object DatabaseFactory {
    const val len = 64
    lateinit var h2db: Database
    fun init(url: String) {
        h2db = Database.connect(url, "org.h2.Driver")
        println("H2 соединён на $url")
        transaction(h2db) {
            SchemaUtils.create(PI)
            SchemaUtils.create(PIAF)
            SchemaUtils.create(PICC)
        }
    }

//    suspend fun <T> dbQuery(block: suspend () -> T): T =
//        newSuspendedTransaction(Dispatchers.IO) { block() }

    fun close() {
        println("H2 отсоединён")
    }

    object PI : Table() {
        val sid = varchar("sid", len)
        override val primaryKey = PrimaryKey(sid)
    }
    object PIAF: Table() {
        val sid = varchar("sid", len)
        val af = varchar("af", len)
        override val primaryKey = PrimaryKey(sid, af)
    }

    object PICC: Table() {
        val sid = varchar("sid", len)
        val PartyID = varchar("PartyID", len)
        val ComponentID = varchar("ComponentID", len)
        val ChannelID = varchar("ChannelID", len)
        override val primaryKey = PrimaryKey(sid, PartyID, ComponentID, ChannelID)
    }
}