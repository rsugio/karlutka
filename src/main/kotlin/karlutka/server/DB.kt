package karlutka.server

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
            SchemaUtils.create(PICC)
            SchemaUtils.create(PIICO)
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
        val sid = varchar("sid", len)
        val af = varchar("af", len)
        // очевидно здесь будут ещё атрибуты
        override val primaryKey = PrimaryKey(sid, af)
    }

    object PICC : Table() {
        val sid = varchar("sid", len)
        val PartyID = varchar("PartyID", len)
        val ComponentID = varchar("ComponentID", len)
        val ChannelID = varchar("ChannelID", len)
        val oid = varchar("oid", guidlen).default("")
        override val primaryKey = PrimaryKey(sid, PartyID, ComponentID, ChannelID)

        fun channels(s: String): List<XiBasis.CommunicationChannelID> {
            return PICC.select(sid eq s)
                .map { XiBasis.CommunicationChannelID(it[PartyID], it[ComponentID], it[ChannelID]) }
        }

        fun insert(s: String, lst: List<XiBasis.CommunicationChannelID>) {
            lst.forEach { cc ->
                PICC.insert {
                    it[sid] = s
                    it[PartyID] = cc.PartyID
                    it[ComponentID] = cc.ComponentID
                    it[ChannelID] = cc.ChannelID
                }
            }
        }
    }

    object PIICO : Table() {
        val sid = varchar("sid", len)
        val SenderPartyID = varchar("SenderPartyID", len)
        val SenderComponentID = varchar("SenderComponentID", len)
        val InterfaceName = varchar("InterfaceName", len)
        val InterfaceNamespace = varchar("InterfaceNamespace", len)
        val ReceiverPartyID = varchar("ReceiverPartyID", len)
        val ReceiverComponentID = varchar("ReceiverComponentID", len)
        val oid = varchar("oid", guidlen).default("")
        override val primaryKey = PrimaryKey(
            sid,
            SenderPartyID,
            SenderComponentID,
            InterfaceName,
            InterfaceNamespace,
            ReceiverPartyID,
            ReceiverComponentID
        )

        fun icos(s: String): List<XiBasis.IntegratedConfigurationID> {
            return PIICO.select(sid eq s)
                .map {
                    XiBasis.IntegratedConfigurationID(
                        it[SenderPartyID], it[SenderComponentID],
                        it[InterfaceName], it[InterfaceNamespace], it[ReceiverPartyID], it[ReceiverComponentID]
                    )
                }
        }

        fun insert(s: String, lst: List<XiBasis.IntegratedConfigurationID>) {
            lst.forEach { ico ->
                PIICO.insert {
                    it[sid] = s
                    it[SenderPartyID] = ico.SenderPartyID
                    it[SenderComponentID] = ico.SenderComponentID
                    it[InterfaceName] = ico.InterfaceName
                    it[InterfaceNamespace] = ico.InterfaceNamespace
                    it[ReceiverPartyID] = ico.ReceiverPartyID
                    it[ReceiverComponentID] = ico.ReceiverComponentID
                }
            }
        }
    }
}