package karlutka.server

import karlutka.models.MPI
import karlutka.parsers.pi.SLD_CIM
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.nio.file.Path
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

    val swcv = mutableListOf<MPI.Swcv>()
    val esrobjects = mutableListOf<MPI.EsrObj>()

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
        readFCPA = conn.prepareStatement("select NAME,XML from PUBLIC.FAE_CPA where sid=?1 and oid=?2")
        readFCPAO = conn.prepareStatement("select TYPEID,XML from PUBLIC.FAE_CPA where sid=?1")
        delFCPA = conn.prepareStatement("delete from PUBLIC.FAE_CPA where sid=?1 and oid=?2")
        updFCPA = conn.prepareStatement("update PUBLIC.FAE_CPA set XML=?3 where SID=?1 and OID=?2")
        insFCPA = conn.prepareStatement("insert into PUBLIC.FAE_CPA(sid,oid,typeid,name,xml) values(?1,?2,?3,?4,?5)")
        //   мониторинг
        insFAEM = conn.prepareStatement("insert into PUBLIC.FAE_MSG(SID,MESSAGEID,DATETIME,SENDER,RECEIVER,BODY) values(?1,?2,?3,?4,?5,?6)")

        //readSwcvList()
//        println("прочитаны SWCV")
//        val usedSw = readEsrObjList()
//        println("прочитаны ESROBJ")
        // чтобы в памяти не держать бессмысленные ссылки, удаляем ненужные SWCV но сохраняем
//        swcv.removeIf { !usedSw.contains(it.guid) }
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

    fun getTpzNumber(path: Path, onNew: (Int) -> Unit): Int {
        val rs = executeQuery(readSrc, path.toString(), false)
        if (rs.next()) return rs.getInt(1)
        val new = executeInsertG(insSrc, path.toString(), false)
        onNew.invoke(new)
        return new
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readSwcvList() {
        val rs = executeQuery(readSwcvList)
        while (rs.next()) {
            val sw = MPI.Swcv(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)
            )
//            require(!swcv.contains(sw))   на больших списках пипец
            swcv.add(sw)
        }
        val js = javaClass.getResourceAsStream("/database/swcv.json")!!
        val _swc = Json.decodeFromStream<List<SLD_CIM.SAP_SoftwareComponent>>(js)
        // смотрим какие стандартные SWCV не в списке и их пишем
        conn.beginRequest()
        _swc.forEach { st ->
            val exist = swcv.find { it.guid == st.GUID }
            if (exist == null) {
                val sw = MPI.Swcv(st.GUID, st.Caption, st.Name, st.Vendor, st.Version, st.Description)
                writeSwcv(sw)
            }
        }
        conn.endRequest()
        conn.commit()
    }

    fun writeSwcv(sw: MPI.Swcv) {
//        require(!swcv.contains(sw))
        try {
            executeUpdateStrict(insswcv, sw.guid, sw.caption, sw.ws_name, sw.vendor, sw.version, sw.description)
        } catch (_: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException) {
            // Уже есть в БД. Читать не пробуем чтобы не тормозило.
        }
        swcv.add(sw)
//        require(swcv.contains(sw))
    }

    private fun readEsrObjList(): Set<String> {
        val rs = executeQuery(readObj)
        val usedsw = hashSetOf<String>()
        while (rs.next()) {
            val eo = MPI.EsrObj(
                MPI.ETypeID.valueOf(rs.getString(2)),
                rs.getString(3),
                rs.getString(4),
                rs.getInt(5),
                rs.getString(6),
                rs.getInt(1),
            )
//            requireNotNull(swcv.find { it.guid == eo.swcvid })
//            require(!esrobjects.contains(eo))
            esrobjects.add(eo)
            usedsw.add(eo.swcvid)
        }
        return usedsw
    }

    fun writeEsrObj(esrobj: MPI.EsrObj) {
//        require(!esrobjects.contains(esrobj))
        esrobj.num = executeInsertG(insobj, esrobj.typeID.toString(), esrobj.oid, esrobj.swcvid, esrobj.swcvsp, esrobj.key)
        esrobjects.add(esrobj)
//        require(esrobjects.contains(esrobj))
    }

    fun writeEsrObjVersion(esrobj: MPI.EsrObj, srcNum: Int, vid: String, text: String?): Int {
//        require(esrobjects.contains(esrobj))
        require(esrobj.num != 0)
        require(srcNum != 0)
        val vernum = executeInsertG(insvers, srcNum, esrobj.num, vid, text)
        return vernum
    }

    fun writeEsrObjLink(vernum: Int, role: String, kpos: Int, linkTo: MPI.EsrObj) {
        require(linkTo.num != 0)
        executeUpdateStrict(inslink, vernum, role, kpos, linkTo.num)
    }

    fun dot1() {
        val ps = conn.prepareStatement(
            """// typeID from | role | typeID to
select src, role, dst, count(*) cnt
   from (select eo.TYPEID  src,
                eo.NUM,
                vl.VERNUM,
                vl.KPOS,
                vl.ROLE,
                vl.OBJNUM,
                eo2.TYPEID dst
         from public.ESRVLINK vl
                  inner join ESRVER EV on vl.VERNUM = EV.NUM
                  inner join ESROBJ EO on EV.OBJNUM = EO.NUM
                  inner join ESROBJ EO2 on VL.OBJNUM = EO2.NUM)
   group by src, role, dst"""
        )
        val rs = executeQuery(ps)
        val log = StringBuilder()
        log.append("digraph finite_state_machine {\nrankdir=TB;\n")
        while (rs.next()) {
            val src = rs.getString(1)
            val role = rs.getString(2)
            val dst = rs.getString(3)
            val cnt = rs.getInt(4)
            log.append("$src -> $dst [label=\"$role: $cnt\"];\n")
        }
        log.append("}\n")
        println(log)
    }

    fun readStore(sql: String, arg1: String): MPI.State? {
//        require(sq.containsKey(sql))
//        val ps = sq[sql]!!
//        ps.setString(1, arg1)
//        if (ps.execute() && ps.resultSet.first()) {
//            val bis = ps.resultSet.getBinaryStream("store")
//            if (bis != null) {
//                return ProtoBuf.decodeFromByteArray<MPI.State>(GZIPInputStream(bis).readAllBytes())
//            }
//        }
        return null
    }

    fun writeStore(sql: String, arg1: String, arg2: MPI.State) {
//        require(sq.containsKey(sql))
//        val ps = sq[sql]!!
//        ps.setString(1, arg1)
//        val bao = ByteArrayOutputStream()
//        val gzo = GZIPOutputStream(bao)
//        gzo.write(ProtoBuf.encodeToByteArray(arg2))
//        gzo.close()
//        bao.close()
//        ps.setBlob(2, ByteArrayInputStream(bao.toByteArray()))
//        require(ps.executeUpdate() == 1)
    }
}

//        val sq = """CREATE TABLE IF NOT EXISTS PI(
//  SID CHARACTER VARYING(32) not null primary key,
//  store BINARY LARGE OBJECT not null
//);
//CREATE TABLE IF NOT EXISTS XIOBJ(
//  foreign key (sid) references PI(SID),
//  SID CHARACTER VARYING(128) not null,
//  OID CHARACTER(32) not null,
//  VID CHARACTER(32) not null,
//  serv CHARACTER VARYING(32) not null,
//  obj CHARACTER VARYING(655360),
//  primary key (SID,OID,VID,serv)
//);
//"""