import KT.Companion.s
import karlutka.parsers.pi.AdapterMessageMonitoringVi.*
import karlutka.serialization.KSoap.Fault
import javax.mail.Header
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import kotlin.test.Test

class KAdapterMessageMonitoringTest {
    @Test
    fun stable() {
        GetUserDefinedSearchFilters().composeSOAP()
        val mkey = "7b761582-1227-11ed-8bad-0000004c2912\\OUTBOUND\\4991250\\EO\\0\\"
        val fault = Fault()
        val filterResponse =
            GetUserDefinedSearchFiltersResponse.parseSOAP(s("pi_AdapterMM/01.xml"), fault)
        require(filterResponse.size == 3 && fault.isSuccess())
        val b = GetUserDefinedSearchFiltersResponse.parseSOAP(s("pi_AdapterMM/02.xml"), fault)
        require(b.size == 0 && fault.faultstring == "Authentication failed. For details see log entry logID=C000AC130D121CF4000000020000116D in security log.")

        val f1 = GetUserDefinedSearchExtractors("COND_A.COND_A04", "urn:sap-com:document:sap:idoc:messages")
        f1.composeSOAP()
        val f2 = GetUserDefinedSearchExtractorsResponse.parseSOAP(s("pi_AdapterMM/03.xml"), fault)
        require(fault.isSuccess() && f2.size == 2)

        val filter = AdapterFilter()
        require(filter.archive == false)
        var s = GetMessageList(filter).composeSOAP()
        require(s != "")
        var l = GetMessageListResponse.parseSOAP(s("pi_AdapterMM/04.xml"), fault)
        require(l?.afw == null)
        l = GetMessageListResponse.parseSOAP(s("pi_AdapterMM/05prev.xml"), fault)
        require(l!!.afw!!.list[0].businessAttributes!!.list!!.size == 3)
        l = GetMessageListResponse.parseSOAP(s("pi_AdapterMM/06fault.xml"), fault)
        require(!fault.isSuccess() && l == null)
        require(fault.faultstring == "Authentication failed. For details see log entry logID=C000AC130D0E9C3A0000000200004360 in security log.")
        val bykeysreq = GetMessagesByKeys(ArrayOfStrings(mkey, mkey), 1000)
        require(bykeysreq.filter.strings.size == 2 && bykeysreq.composeSOAP().isNotBlank())

        val bykeysresp = GetMessagesByKeysResponse.parseSOAP(s("pi_AdapterMM/07bykeys.xml"), fault)
        require(fault.isSuccess() && bykeysresp != null && bykeysresp.afw!!.list.size == 2)

        val uds1 =
            GetUserDefinedSearchAttributes(mkey, false)
        require(uds1.composeSOAP().isNotBlank())
        var uds2 =
            GetUserDefinedSearchAttributesResponse.parseSOAP(s("pi_AdapterMM/08attrs.xml"), fault)
        require(uds2.size == 0)
        uds2 = GetUserDefinedSearchAttributesResponse.parseSOAP(s("pi_AdapterMM/09attrs.xml"), fault)
        require(uds2.size == 2)

        val log1 = GetLogEntries(
            mkey, false, null, "2022-08-01T23:00:00+03:00"
        )
        require(log1.composeSOAP().isNotBlank())
        var log2 = GetLogEntriesResponse.parseSOAP(s("pi_AdapterMM/10log.xml"), fault)
        require(log2.size == 0)
        log2 = GetLogEntriesResponse.parseSOAP(s("pi_AdapterMM/11log.xml"), fault)
        require(log2.size > 0)

        s = GetLoggedMessageBytes(mkey, "2").composeSOAP()
        require(s.isNotBlank())
        var log3 = GetLoggedMessageBytesResponse.parseSOAP(s("pi_AdapterMM/12bytes.xml"), fault)
        require(fault.isSuccess() && log3.isEmpty())
        log3 = GetLoggedMessageBytesResponse.parseSOAP(s("pi_AdapterMM/13bytes.xml"), fault)
        require(fault.isSuccess() && log3.isNotEmpty())

        s = ResendMessages(ArrayOfStrings(mkey)).composeSOAP()
        require(s.isNotBlank())
        val r = ResendMessagesResponse.parseSOAP(s("pi_AdapterMM/14resend.xml"), fault)
        require(r != null && fault.isSuccess())
        s = CancelMessages(ArrayOfStrings(mkey)).composeSOAP()
        require(s.isNotBlank())
        val cr = CancelMessagesResponse.parseSOAP(s("pi_AdapterMM/15cancel.xml"), fault)
        require(cr != null && fault.isSuccess())

        s = GetUserDefinedSearchMessages(
            AdapterFilter(), null,
            ArrayOfBusinessAttribute(mutableListOf(BusinessAttribute("A", "B"))), "AND"
        ).composeSOAP()
        require(s.isNotBlank())
        var uds3 = GetUserDefinedSearchMessagesResponse.parseSOAP(s("pi_AdapterMM/16uds.xml"), fault)
        require(uds3 != null && uds3.number == 0 && fault.isSuccess())
        uds3 = GetUserDefinedSearchMessagesResponse.parseSOAP(s("pi_AdapterMM/17uds.xml"), fault)
        require(uds3 != null && uds3.number == 1 && fault.isSuccess() && uds3.afw!!.list.size == 1)

        s = GetMessagesByIDs(ArrayOfStrings("7b761582-1227-11ed-8bad-0000004c2912")).composeSOAP()
        require(s.isNotBlank())
        var ids = GetMessagesByIDsResponse.parseSOAP(s("pi_AdapterMM/18ids.xml"), fault)
        require(fault.isFailure() && fault.faultstring == "Cannot find the required parameter [referenceIds] in request message content." && ids == null)
        ids = GetMessagesByIDsResponse.parseSOAP(s("pi_AdapterMM/19ids.xml"), fault)
        require(fault.isSuccess() && ids != null && ids.number == 0)
        ids = GetMessagesByIDsResponse.parseSOAP(s("pi_AdapterMM/20ids.xml"), fault)
        require(fault.isSuccess() && ids != null && ids.number == 1 && ids.afw!!.list.size == 1)

        s = GetMessageBytesJavaLangStringBoolean("mkey").composeSOAP()
        require(s.isNotBlank())
        val sbr =
            GetMessageBytesJavaLangStringBooleanResponse.parseSOAP(s("pi_AdapterMM/21sbr.xml"), fault)
        require(sbr.size > 1 && fault.isSuccess())
        s = GetMessageBytesJavaLangStringIntBoolean(mkey, 1).composeSOAP()
        require(s.isNotBlank())
        val sbir = GetMessageBytesJavaLangStringIntBooleanResponse.parseSOAP(
            s("pi_AdapterMM/22sbir.xml"),
            fault
        )
        require(sbir.size > 1 && fault.isSuccess())

        s = FailEoioMessage(mkey, true).composeSOAP()
        require(s.isNotBlank())
        var fail = FailEoioMessageResponse.parseSOAP(s("pi_AdapterMM/23fail.xml"), fault)
        require(fault.isSuccess() && fail!!.resultCode == "02")
        fail = FailEoioMessageResponse.parseSOAP(s("pi_AdapterMM/24fail.xml"), fault)
        require(fault.isFailure() && fail == null)

        s = GetAllAvailableStatusDetails(LocaleAMM("en")).composeSOAP()
        require(s.isNotBlank())
        val ga =
            GetAllAvailableStatusDetailsResponse.parseSOAP(s("pi_AdapterMM/25statuses.xml"), fault)
        require(ga.size == 185 && ga[0].errorLabelID == "6401")

        s = GetConnections().composeSOAP()
        require(s.isNotBlank())
        val conns = GetConnectionsResponse.parseSOAP(s("pi_AdapterMM/26conns.xml"), fault)
        require(conns.size == 29 && conns[0] == "AFW")

        s = GetErrorCodes(6401).composeSOAP()
        require(s.isNotBlank())
        val ec = GetErrorCodesResponse.parseSOAP(s("pi_AdapterMM/27error.xml"), fault)
        require(ec.size == 1 && ec[0] == "ODATA_INITIALIZATION_ERROR")

        s = GetIntegrationFlows("EN").composeSOAP()
        require(s.isNotBlank())
        val flows = GetIntegrationFlowResponse.parseSOAP(s("pi_AdapterMM/28flows.xml"), fault)
        require(fault.isSuccess() && flows.size == 2 && flows[0].name == "|SID400|SI_OutSync||")

        s = GetInterfaces().composeSOAP()
        require(s.isNotBlank())
        var iface = GetInterfacesResponse.parseSOAP(s("pi_AdapterMM/29iface.xml"), fault)
        require(iface == null)  //TODO пустой сервис

        s = GetMessagesWithSuccessors(ArrayOfStrings("7b761582-1227-11ed-8bad-0000004c2912")).composeSOAP()
        require(s.isNotBlank())
        val suc = GetMessagesWithSuccessorsResponse.parseSOAP(s("pi_AdapterMM/30suc.xml"), fault)
        require(suc != null && fault.isSuccess() && suc.afw!!.list.size == 1)

        s = GetParties().composeSOAP()
        require(s.isNotBlank())
        val part = GetPartiesResponse.parseSOAP(s("pi_AdapterMM/31part.xml"), fault)
        require(part == null && fault.isSuccess()) //TODO пустой сервис

        s = GetPredecessorMessageId("7b761582-1227-11ed-8bad-0000004c2912", "OUTBOUND").composeSOAP()
        require(s.isNotBlank())
        val pred = GetPredecessorMessageIdResponse.parseSOAP(s("pi_AdapterMM/32pred.xml"), fault)
        require(pred == null && fault.isSuccess())

        s = GetServices().composeSOAP()
        require(s.isNotBlank())
        val serv = GetServicesResponse.parseSOAP(s("pi_AdapterMM/33serv.xml"), fault)
        require(serv == null && fault.isSuccess())//TODO пустой сервис

        s = GetStatusDetails(ArrayOfStrings("ODATA_INITIALIZATION_ERROR"), LocaleAMM("en")).composeSOAP()
        require(s.isNotBlank())
        val stat = GetStatusDetailsResponse.parseSOAP(s("pi_AdapterMM/34stat.xml"), fault)
        require(fault.isSuccess() && stat.size == 1 && stat[0].errorLabelID == "6401")
    }

    @Test
    fun mime() {
        val ds = ByteArrayDataSource(s("pi_AdapterMM/mime.txt"), "multipart/related")
        val mm = MimeMultipart(ds)
        var i = 0
        while (i < mm.count) {
            val bp = mm.getBodyPart(i)
            for (hd in bp.allHeaders) {
                require(hd is Header)
                if (false) println("${hd.name}\t${hd.value}")
            }
            if (false) println(bp.inputStream.readAllBytes().size)
            i++
        }
        require(i == 2)
    }
}