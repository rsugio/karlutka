package karlutka.server

import karlutka.clients.PI
import karlutka.parsers.pi.Cim
import karlutka.parsers.pi.SLD_CIM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import java.net.URI

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
    private val sldHost: String
    private val namespacepath: Cim.NAMESPACEPATH

    init {
        val rs1 = DB.executeQuery(DB.readFAE, sid)
        if (!rs1.next()) {
            DB.executeInsert(DB.insFAE, sid, afFaHostdb)
        }
        sldHost = getSldServer()
        namespacepath = Cim.NAMESPACEPATH(sldHost, SLD_CIM.sldactive)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getSldServer(): String {
        return runBlocking {
            val t = sld.sldop(SLD_CIM.SAPExt_GetObjectServer(), GlobalScope).await()
            SLD_CIM.SAPExt_GetObjectServer_resp(Cim.decodeFromReader(t.bodyAsXmlReader()))
        }
    }

    private fun urlOf(s: String = ""): String {
        return "$realHostPortURI/$s"
    }

    /**
     * Регистрирует FAE в SLD
     * Если domain непустой то добавляет также в него
     */
    suspend fun registerSLD(domain: String?, scope: CoroutineScope) {
        // Найти родительский домен

        var x = SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_XIDomain)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        val domains = x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE
        // <VALUE.NAMEDINSTANCE>
        //	<INSTANCENAME CLASSNAME="SAP_XIDomain">
        //		<KEYBINDING NAME="CreationClassName">
        //			<KEYVALUE>SAP_XIDomain</KEYVALUE>
        //		</KEYBINDING>
        //		<KEYBINDING NAME="Name">
        //			<KEYVALUE>domain.01.nl-s-pihvscs</KEYVALUE>
        //		</KEYBINDING>
        //	</INSTANCENAME>
        //	<INSTANCE CLASSNAME="SAP_XIDomain"/>
        // </VALUE.NAMEDINSTANCE>
        val foundDomain = domains.find { d -> d.INSTANCENAME.getKeyValue("Name") == domain }?.INSTANCENAME

        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(afFaHostdb)
        val af1 = sld.sldop(SLD_CIM.createInstance(afname, mapOf("Caption" to "Adapter Engine on $afFaHostdb")), scope)
        val af1rez = Cim.decodeFromReader(af1.await().bodyAsXmlReader())
        require(af1rez.isCreatedOrAlreadyExists())
        if (domain != null && foundDomain != null) {
            // запрошенный домен действительно существует
            x = Cim.association("GroupComponent", foundDomain,
                "PartComponent", afname,
                "SAP_XIContainedAdapter", namespacepath)
            val domainrez = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(domainrez.isCreatedOrAlreadyExists()) {domainrez.MESSAGE?.SIMPLERSP?.IMETHODRESPONSE?.ERROR.toString()}
        }

        val admintoolname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "AdminTool.$afFaHostdb")
        x = SLD_CIM.createInstance(admintoolname, mapOf("Caption" to "AdminTool of $afFaHostdb", "Purpose" to "AdminTool"))
        val admtool = sld.sldop(x, scope)

        val cacherefreshname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "CacheRefresh.$afFaHostdb")
        x = SLD_CIM.createInstance(cacherefreshname, mapOf("Caption" to "CacheRefresh of $afFaHostdb", "Purpose" to "CacheRefresh"))
        val cacherefresh = sld.sldop(x, scope)

        val RTCname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "RuntimeCheck.$afFaHostdb")
        val rtc = sld.sldop(SLD_CIM.createInstance(RTCname, mapOf("Caption" to "RuntimeCheck of $afFaHostdb", "Purpose" to "RuntimeCheck")), scope)

        val atrez = Cim.decodeFromReader(admtool.await().bodyAsXmlReader())
        require(atrez.isCreatedOrAlreadyExists())
        val crrez = Cim.decodeFromReader(cacherefresh.await().bodyAsXmlReader())
        require(crrez.isCreatedOrAlreadyExists())
        val rtcrez = Cim.decodeFromReader(rtc.await().bodyAsXmlReader())
        require(rtcrez.isCreatedOrAlreadyExists())

        // создаём http-порты к сервисам
        val proto = realHostPortURI.scheme
        val portbasicurlname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "basicURLs")
        x = SLD_CIM.createInstance(
            portbasicurlname, mapOf(
                "Caption" to "Basic URLs of Adapter Engine $afFaHostdb", "Protocol" to proto, "SecureURL" to urlOf(), "URL" to urlOf()
            )
        )
        val portbasic = sld.sldop(x, scope)
        val portadmintoolname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.AdminTool.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portadmintoolname, mapOf(
                "Caption" to "Port for AdminTool of $afFaHostdb", "Protocol" to proto, "SecureURL" to urlOf("/mdt"), "URL" to urlOf("/mdt")
            )
        )
        val portadmin = sld.sldop(x, scope)
        val portcacherefreshname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.CacheRefresh.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portcacherefreshname, mapOf(
                "Caption" to "Port for CacheRefresh of $afFaHostdb",
                "Protocol" to proto,
                "SecureURL" to urlOf("/CPACache/invalidate"),
                "URL" to urlOf("/CPACache/invalidate")
            )
        )
        val portcr = sld.sldop(x, scope)
        val portRTCname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.RuntimeCheck.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portRTCname, mapOf(
                "Caption" to "Port for RuntimeCheck of $afFaHostdb",
                "Protocol" to proto,
                "SecureURL" to urlOf("/AdapterFramework/rtc"),
                "URL" to urlOf("/AdapterFramework/rtc")
            )
        )
        val portrtc = sld.sldop(x, scope)
        val portbasicrez = Cim.decodeFromReader(portbasic.await().bodyAsXmlReader())
        require(portbasicrez.isCreatedOrAlreadyExists())
        val portadminrez = Cim.decodeFromReader(portadmin.await().bodyAsXmlReader())
        require(portadminrez.isCreatedOrAlreadyExists())
        val portcrrez = Cim.decodeFromReader(portcr.await().bodyAsXmlReader())
        require(portcrrez.isCreatedOrAlreadyExists())
        val portrtcrez = Cim.decodeFromReader(portrtc.await().bodyAsXmlReader())
        require(portrtcrez.isCreatedOrAlreadyExists())

        // Делаем ассоциации
        // AdminTool.af.fa0.fake0db -> XIAF
        x = Cim.association("Antecedent", afname, "Dependent", admintoolname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        // AdminTool.af.fa0.fake0db -> HTTPport
        x = Cim.association("Antecedent", admintoolname, "Dependent", portadmintoolname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        // four more
        x = Cim.association("Antecedent", afname, "Dependent", cacherefreshname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", cacherefreshname, "Dependent", portcacherefreshname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", afname, "Dependent", RTCname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", RTCname, "Dependent", portRTCname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        //basic
        x = Cim.association("Antecedent", afname, "Dependent", portbasicurlname, "SAP_XIAdapterHostedHTTPServicePort", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())

        return
    }

}