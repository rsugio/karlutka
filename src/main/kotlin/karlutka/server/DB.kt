package karlutka.server

import karlutka.models.MPI
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
    lateinit var readswcv: PreparedStatement
    lateinit var insobj: PreparedStatement
    lateinit var readObj: PreparedStatement
    lateinit var insvers: PreparedStatement
    lateinit var inslink: PreparedStatement

    val swcv = mutableListOf<MPI.Swcv>()
    val esrobjects = mutableListOf<MPI.EsrObj>()

    fun init(url: String) {
        conn = DriverManager.getConnection(url)
        val schema = javaClass.getResource("/database/h2.sql")!!.readText()
        conn.prepareStatement(schema).execute()
        require(conn.isValid(3))
        readSrc = conn.prepareStatement("SELECT num FROM PUBLIC.SRC where ONLINE=?2 and path=?1")
        insSrc = conn.prepareStatement("INSERT INTO PUBLIC.SRC(PATH,ONLINE) VALUES (?1,?2)", 1)
        insswcv =
            conn.prepareStatement("INSERT INTO PUBLIC.SWCV(GUID,CAPTION,WS_NAME,VENDOR,VERSION) VALUES(?1,?2,?3,?4,?5)")
        readswcv = conn.prepareStatement("SELECT * FROM PUBLIC.SWCV")
        insobj = conn.prepareStatement(
            "INSERT INTO PUBLIC.ESROBJ(TYPEID,OID,SWCVID,SWCVSP,KEY_) VALUES(?1,?2,?3,?4,?5)", 1
        )
        readObj = conn.prepareStatement("SELECT NUM,TYPEID,OID,SWCVID,SWCVSP,KEY_ FROM PUBLIC.ESROBJ")
        insvers = conn.prepareStatement("INSERT INTO PUBLIC.ESRVER(SRCNUM,OBJNUM,VID,TEXT) VALUES(?1,?2,?3,?4)", 1)
        inslink = conn.prepareStatement("INSERT INTO PUBLIC.ESRVLINK(VERNUM,ROLE,KPOS,OBJNUM) VALUES(?1,?2,?3,?4)")
        println("H2 соединён на $url")

        readSwcv()
        readEsrObj()
    }

    fun close() {
        conn.close()
        println("H2 отсоединён")
    }

    private fun setArgs(ps: PreparedStatement, vararg args: Any?) {
        var ix = 1
        for (arg in args) {
            // может есть готовый метод?
            if (arg == null) {
                ps.setNull(ix++, java.sql.Types.VARCHAR)
            } else if (arg is String)
                ps.setString(ix++, arg)
            else if (arg is Boolean)
                ps.setBoolean(ix++, arg)
            else if (arg is Int)
                ps.setInt(ix++, arg)
            else error(arg.javaClass)
        }
    }

    private fun executeQuery(ps: PreparedStatement, vararg args: Any?): ResultSet {
        setArgs(ps, *args)
        return ps.executeQuery()
    }

    private fun executeInsertG(ps: PreparedStatement, vararg args: Any?): Int {
        setArgs(ps, *args)
        require(ps.executeUpdate() == 1)
        require(ps.generatedKeys.next())
        return ps.generatedKeys.getInt(1)
    }

    private fun executeInsert(ps: PreparedStatement, vararg args: Any?) {
        setArgs(ps, *args)
        require(ps.executeUpdate() == 1)
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

    private fun readSwcv() {
        val rs = executeQuery(readswcv)
        while (rs.next()) {
            val sw = MPI.Swcv(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5))
            require(!swcv.contains(sw))
            swcv.add(sw)
        }
    }

    fun writeSwcv(sw: MPI.Swcv) {
        require(!swcv.contains(sw))
        executeInsert(insswcv, sw.guid, sw.caption, sw.ws_name, sw.vendor, sw.version)
        swcv.add(sw)
        require(swcv.contains(sw))
    }

    private fun readEsrObj() {
        readObj = conn.prepareStatement("SELECT NUM,TYPEID,OID,SWCVID,SWCVSP,KEY_ FROM PUBLIC.ESROBJ")

        val rs = executeQuery(readObj)
        while (rs.next()) {
            val eo = MPI.EsrObj(
                MPI.ETypeID.valueOf(rs.getString(2)),
                rs.getString(3),
                rs.getString(4),
                rs.getInt(5),
                rs.getString(6),
                rs.getInt(1),
            )
            requireNotNull(swcv.find { it.guid == eo.swcvid })       //TODO на равенство одному
            require(!esrobjects.contains(eo))
            esrobjects.add(eo)
        }
    }

    fun writeEsrObj(esrobj: MPI.EsrObj) {
        require(!esrobjects.contains(esrobj))
        esrobj.num =
            executeInsertG(insobj, esrobj.typeID.toString(), esrobj.oid, esrobj.swcvid, esrobj.swcvsp, esrobj.key)
        esrobjects.add(esrobj)
        require(esrobjects.contains(esrobj))
    }

    fun writeEsrObjVersion(esrobj: MPI.EsrObj, srcNum: Int, vid: String, text: String?): Int {
        require(esrobjects.contains(esrobj))
        require(esrobj.num != 0)
        require(srcNum != 0)
        val vernum = executeInsertG(insvers, srcNum, esrobj.num, vid, text)
        return vernum
    }

    fun writeEsrObjLink(vernum: Int, role: String, kpos: Int, linkTo: MPI.EsrObj) {
        require(linkTo.num != 0)
        executeInsert(inslink, vernum, role, kpos, linkTo.num)
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