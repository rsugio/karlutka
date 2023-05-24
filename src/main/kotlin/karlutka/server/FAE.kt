@file:OptIn(DelicateCoroutinesApi::class, DelicateCoroutinesApi::class)

package karlutka.server

import karlutka.clients.PI
import karlutka.parsers.pi.Cim
import karlutka.parsers.pi.SLD_CIM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.*

/**
 * Fake Adapter Engine
 */
data class FAE(
    val sid: String,
    val fakehostdb: String,
    val realHostPortURI: URI,
    val cae: PI,
    val sld: PI,
) {
    val afFaHostdb = "af.$sid.$fakehostdb".lowercase()
    val sldprops = Properties()
    val sldHost: String
    val namespacepath: Cim.NAMESPACEPATH

    init {
        val rs1 = DB.executeQuery(DB.readFAE, sid)
        if (!rs1.next()) {
            DB.executeInsert(DB.insFAE, sid, afFaHostdb)
        }
        sldHost = getSldServer()
        namespacepath = Cim.NAMESPACEPATH(sldHost, SLD_CIM.sldactive)
    }

    fun getSldServer(): String {
        return runBlocking {
            val t = sld.sldop(SLD_CIM.SAPExt_GetObjectServer(), GlobalScope).await()
            SLD_CIM.SAPExt_GetObjectServer_resp(Cim.decodeFromReader(t.bodyAsXmlReader()))
        }
    }

    suspend fun registerSLD(scope: CoroutineScope) {
        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(afFaHostdb)
        val afname1 = sld.sldop(SLD_CIM.createInstance(afname, mapOf("Caption" to "Adapter Engine 123 on $afFaHostdb")), scope)

        return
    }

    fun asx(
        prFrom: String,
        instFrom: Cim.INSTANCENAME,
        prTo: String,
        instTo: Cim.INSTANCENAME,
        assClass: String,
        namespacepath: Cim.NAMESPACEPATH,
    ): Cim.CIM {
        val f1 = Cim.createPropertyReference(prFrom, instFrom.CLASSNAME, Cim.INSTANCEPATH(namespacepath, instFrom))
        val t1 = Cim.createPropertyReference(prTo, instTo.CLASSNAME, Cim.INSTANCEPATH(namespacepath, instTo))
        val ca = Cim.createAssociation(assClass, f1, t1)
        return SLD_CIM.createInstance(ca)
    }
}