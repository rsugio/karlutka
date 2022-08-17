package karlutka.server

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    lateinit var h2db: Database
    fun init(url: String) {
        h2db = Database.connect(url, "org.h2.Driver")
        println("H2 соединён на $url")
//        transaction(h2db) {
//            SchemaUtils.create(Articles)
//        }
    }

//    suspend fun <T> dbQuery(block: suspend () -> T): T =
//        newSuspendedTransaction(Dispatchers.IO) { block() }

    fun close() {
//        println("H2 отсоединён")
    }
}