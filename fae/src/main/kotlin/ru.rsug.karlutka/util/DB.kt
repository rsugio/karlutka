package ru.rsug.karlutka.util

import java.io.InputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

object DB {
    lateinit var conn: Connection
    lateinit var readSrc: PreparedStatement
    lateinit var insSrc: PreparedStatement
    lateinit var insswcv: PreparedStatement
    lateinit var readSwcvList: PreparedStatement
    lateinit var readSwcv: PreparedStatement
    lateinit var insobj: PreparedStatement
    lateinit var readObj: PreparedStatement
    lateinit var insvers: PreparedStatement
    lateinit var inslink: PreparedStatement
    lateinit var readFAE: PreparedStatement
    lateinit var insFAE: PreparedStatement
    lateinit var readFCPA: PreparedStatement
    lateinit var readFCPAO: PreparedStatement
    lateinit var delFCPA: PreparedStatement
    lateinit var insFCPA: PreparedStatement
    lateinit var updFCPA: PreparedStatement
    lateinit var insFAEM: PreparedStatement
    lateinit var selectFAEM: PreparedStatement
    lateinit var clearFAE: PreparedStatement
    lateinit var insFAECPHIST: PreparedStatement
    lateinit var selectFAECPHIST: PreparedStatement

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
        val schema = javaClass.getResource("/database/h2.sql")!!.readText()
        conn.prepareStatement(schema).execute()
        readSrc = conn.prepareStatement("select num from PUBLIC.SRC where online=?2 and path=?1")
        insSrc = conn.prepareStatement("insert into PUBLIC.SRC(path,online) values (?1,?2)", 1)
        insswcv = conn.prepareStatement("insert into PUBLIC.SWCV(guid,caption,ws_name,vendor,version,description) values(?1,?2,?3,?4,?5,?6)")
        readSwcvList = conn.prepareStatement("select guid,caption,ws_name,vendor,version,description from PUBLIC.SWCV")
        readSwcv = conn.prepareStatement("select caption,ws_name,vendor,version,description from PUBLIC.SWCV where guid=?1")
        insobj = conn.prepareStatement("insert into PUBLIC.ESROBJ(typeid,oid,swcvid,swcvsp,key_) values(?1,?2,?3,?4,?5)", 1)
        readObj = conn.prepareStatement("select num,typeid,oid,swcvid,swcvsp,key_ from PUBLIC.ESROBJ")
        insvers = conn.prepareStatement("insert into PUBLIC.ESRVER(srcnum,objnum,vid,text) values(?1,?2,?3,?4)", 1)
        inslink = conn.prepareStatement("insert into PUBLIC.ESRVLINK(vernum,role,kpos,objnum) values(?1,?2,?3,?4)")
        // FAE
        readFAE = conn.prepareStatement("select sid, info from PUBLIC.FAE where sid=?")
        insFAE = conn.prepareStatement("insert into PUBLIC.FAE(sid,info) values(?1,?2)")
        readFCPA = conn.prepareStatement("select NAME,XML,DATETIME from PUBLIC.FAE_CPA where sid=?1 and oid=?2")
        readFCPAO = conn.prepareStatement("select TYPEID,XML,OID,NAME,DATETIME from PUBLIC.FAE_CPA where sid=?1")
        delFCPA = conn.prepareStatement("delete from PUBLIC.FAE_CPA where sid=?1 and oid=?2")
        updFCPA = conn.prepareStatement("update PUBLIC.FAE_CPA set XML=?3, DATETIME=?4 where SID=?1 and OID=?2")
        insFCPA = conn.prepareStatement("insert into PUBLIC.FAE_CPA(sid,oid,typeid,name,xml,DATETIME) values(?1,?2,?3,?4,?5,?6)")
        // FAE мониторинг
        insFAEM = conn.prepareStatement("insert into PUBLIC.FAE_MSG(SID,ROUTEID,MESSAGEID,DATETIME,SENDER,RECEIVER,BODY) values(?1,?2,?3,?4,?5,?6,?7)")
        selectFAEM = conn.prepareStatement("select ROUTEID,MESSAGEID,DATETIME,SENDER,RECEIVER,BODY from PUBLIC.FAE_MSG where sid=?1 order by DATETIME DESC")
        clearFAE = conn.prepareStatement("delete PUBLIC.FAE_CPA; delete PUBLIC.FAE_MSG;")
        // FAE история
        insFAECPHIST = conn.prepareStatement("insert into PUBLIC.FAE_CPAHISTORY(SID,DATETIME,FILENAME,REMARK) values(?1,?2,?3,?4)")
        selectFAECPHIST = conn.prepareStatement("select DATETIME,FILENAME,REMARK from PUBLIC.FAE_CPAHISTORY where SID=?1 order by DATETIME DESC")
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

    fun setArgs(ps: PreparedStatement, vararg args: Any?) {
        var ix = 1
        for (arg in args) {
            when (arg) {
                null -> ps.setNull(ix++, java.sql.Types.VARCHAR)
                is String -> ps.setString(ix++, arg)
                is Boolean -> ps.setBoolean(ix++, arg)
                is Int -> ps.setInt(ix++, arg)
                is Long -> ps.setLong(ix++, arg)
                is ByteArray -> ps.setBlob(ix++, arg.inputStream())
                is InputStream -> ps.setBlob(ix++, arg)
                else -> error(arg.javaClass)
            }
        }
    }

    fun executeQuery(ps: PreparedStatement, vararg args: Any?): ResultSet {
        setArgs(ps, *args)
        return ps.executeQuery()
    }

    fun executeInsertG(ps: PreparedStatement, vararg args: Any?): Int {
        setArgs(ps, *args)
        require(ps.executeUpdate() == 1)
        require(ps.generatedKeys.next())
        return ps.generatedKeys.getInt(1)
    }

    fun executeUpdateStrict(ps: PreparedStatement, vararg args: Any?) {
        setArgs(ps, *args)
        val i = ps.executeUpdate()
        require(i > 0) { "Cannot update $ps, got no updates/deletes" }
    }

    fun executeUpdate(ps: PreparedStatement, vararg args: Any?): Int {
        setArgs(ps, *args)
        return ps.executeUpdate()
    }

    fun getPiClientNumber(sid: String, onNew: (Int) -> Unit): Int {
        val rs = executeQuery(readSrc, sid, true)
        if (rs.next()) return rs.getInt(1)
        val new = executeInsertG(insSrc, sid, true)
        onNew.invoke(new)
        return new
    }
}
