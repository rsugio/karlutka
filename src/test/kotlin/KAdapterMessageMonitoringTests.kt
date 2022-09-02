import KT.Companion.x
import karlutka.parsers.pi.AdapterMessageMonitoringVi.*
import karlutka.serialization.KSoap
import karlutka.serialization.KSoap.Fault
import javax.mail.Header
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import kotlin.test.Test

class KAdapterMessageMonitoringTests {
    @Test
    fun stable() {
        GetUserDefinedSearchFilters().composeSOAP()
        val mkey = "7b761582-1227-11ed-8bad-0000004c2912\\OUTBOUND\\4991250\\EO\\0\\"
        val fault = Fault()
        val userfilter = KSoap.parseSOAP<GetUserDefinedSearchFiltersResponse>(x("/pi_AdapterMM/01.xml"), fault)
        require(userfilter!!.Response.MessageInterface.size == 3 && fault.isSuccess())
        val b = KSoap.parseSOAP<GetUserDefinedSearchFiltersResponse>(x("/pi_AdapterMM/02.xml"), fault)
        require(b == null && fault.faultstring.startsWith("Authentication failed"))

        val f1 = GetUserDefinedSearchExtractors("COND_A.COND_A04", "urn:sap-com:document:sap:idoc:messages")
        f1.composeSOAP()
        val f2 = KSoap.parseSOAP<GetUserDefinedSearchExtractorsResponse>(x("/pi_AdapterMM/03.xml"), fault)
        require(fault.isSuccess() && f2!!.Response.AttributeMetadata.size == 2)

        val filter = AdapterFilter()
        require(!filter.archive)
        var s = GetMessageList(filter).composeSOAP()
        require(s != "")
        var l = KSoap.parseSOAP<GetMessageListResponse>(x("/pi_AdapterMM/04.xml"), fault)
        require(l!!.Resp.afw == null)
        l = KSoap.parseSOAP<GetMessageListResponse>(x("/pi_AdapterMM/05prev.xml"), fault)
        require(l!!.Resp.afw!!.list[0].businessAttributes!!.list.size == 3)
        l = KSoap.parseSOAP<GetMessageListResponse>(x("/pi_AdapterMM/06fault.xml"), fault)
        require(!fault.isSuccess() && l == null)
        require(fault.faultstring.startsWith("Authentication failed"))
        val bykeysreq = GetMessagesByKeys(ArrayOfStrings(mkey, mkey), 1000)
        require(bykeysreq.filter.strings.size == 2 && bykeysreq.composeSOAP().isNotBlank())

        val bykeysresp = KSoap.parseSOAP<GetMessagesByKeysResponse>(x("/pi_AdapterMM/07bykeys.xml"), fault)
        require(fault.isSuccess() && bykeysresp!!.Resp.afw!!.list.size == 2)

        val uds1 = GetUserDefinedSearchAttributes(mkey, false)
        require(uds1.composeSOAP().isNotBlank())
        var uds2 = KSoap.parseSOAP<GetUserDefinedSearchAttributesResponse>(x("/pi_AdapterMM/08attrs.xml"), fault)
        require(uds2!!.Resp.list.isEmpty())
        uds2 = KSoap.parseSOAP<GetUserDefinedSearchAttributesResponse>(x("/pi_AdapterMM/09attrs.xml"), fault)
        require(uds2!!.Resp.list.size == 2)

        val log1 = GetLogEntries(
            mkey, false, null, "2022-08-01T23:00:00+03:00"
        )
        require(log1.composeSOAP().isNotBlank())
        var log2 = KSoap.parseSOAP<GetLogEntriesResponse>(x("/pi_AdapterMM/10log.xml"), fault)
        require(log2!!.get().isEmpty())
        log2 = KSoap.parseSOAP<GetLogEntriesResponse>(x("/pi_AdapterMM/11log.xml"), fault)
        require(log2!!.get().size > 10)

        s = GetLoggedMessageBytes(mkey, "2").composeSOAP()
        require(s.isNotBlank())
        var log3 = KSoap.parseSOAP<GetLoggedMessageBytesResponse>(x("/pi_AdapterMM/12bytes.xml"), fault)
        require(fault.isSuccess() && log3!!.Response.isEmpty())
        log3 = KSoap.parseSOAP<GetLoggedMessageBytesResponse>(x("/pi_AdapterMM/13bytes.xml"), fault)
        require(fault.isSuccess() && log3!!.Response.isNotEmpty())

        s = ResendMessages(ArrayOfStrings(mkey)).composeSOAP()
        require(s.isNotBlank())
        val r = KSoap.parseSOAP<ResendMessagesResponse>(x("/pi_AdapterMM/14resend.xml"), fault)
        require(r != null && fault.isSuccess())
        s = CancelMessages(ArrayOfStrings(mkey)).composeSOAP()
        require(s.isNotBlank())
        val cr = KSoap.parseSOAP<CancelMessagesResponse>(x("/pi_AdapterMM/15cancel.xml"), fault)
        require(cr != null && fault.isSuccess())

        s = GetUserDefinedSearchMessages(
            AdapterFilter(), null, ArrayOfBusinessAttribute(mutableListOf(BusinessAttribute("A", "B"))), "AND"
        ).composeSOAP()
        require(s.isNotBlank())
        var uds3 = KSoap.parseSOAP<GetUserDefinedSearchMessagesResponse>(x("/pi_AdapterMM/16uds.xml"), fault)
        require(uds3!!.Resp.number == 0 && fault.isSuccess())
        uds3 = KSoap.parseSOAP<GetUserDefinedSearchMessagesResponse>(x("/pi_AdapterMM/17uds.xml"), fault)
        require(uds3!!.Resp.number == 1 && fault.isSuccess() && uds3.Resp.afw!!.list.size == 1)

        s = GetMessagesByIDs(ArrayOfStrings("7b761582-1227-11ed-8bad-0000004c2912")).composeSOAP()
        require(s.isNotBlank())
        var ids = KSoap.parseSOAP<GetMessagesByIDsResponse>(x("/pi_AdapterMM/18ids.xml"), fault)
        require(!fault.isSuccess() && fault.faultstring.startsWith("Cannot find the required parameter [referenceIds]") && ids == null)
        ids = KSoap.parseSOAP<GetMessagesByIDsResponse>(x("/pi_AdapterMM/19ids.xml"), fault)
        require(fault.isSuccess() && ids!!.Resp.number == 0)
        ids = KSoap.parseSOAP<GetMessagesByIDsResponse>(x("/pi_AdapterMM/20ids.xml"), fault)
        require(fault.isSuccess() && ids!!.Resp.number == 1 && ids.Resp.afw!!.list.size == 1)

        s = GetMessageBytesJavaLangStringBoolean("mkey").composeSOAP()
        require(s.isNotBlank())
        val sbr = KSoap.parseSOAP<GetMessageBytesJavaLangStringBooleanResponse>(x("/pi_AdapterMM/21sbr.xml"), fault)
        require(sbr!!.Response.isNotBlank() && fault.isSuccess())
        s = GetMessageBytesJavaLangStringIntBoolean(mkey, 1).composeSOAP()
        require(s.isNotBlank())
        val sbir = KSoap.parseSOAP<GetMessageBytesJavaLangStringIntBooleanResponse>(
            x("/pi_AdapterMM/22sbir.xml"), fault
        )
        require(sbir!!.Response.isNotEmpty() && fault.isSuccess())

        s = FailEoioMessage(mkey, true).composeSOAP()
        require(s.isNotBlank())
        var fail = KSoap.parseSOAP<FailEoioMessageResponse>(x("/pi_AdapterMM/23fail.xml"), fault)
        require(fault.isSuccess() && fail!!.Response.resultCode == "02")
        fail = KSoap.parseSOAP<FailEoioMessageResponse>(x("/pi_AdapterMM/24fail.xml"), fault)
        require(!fault.isSuccess() && fail == null)

        s = GetAllAvailableStatusDetails(LocaleAMM("en")).composeSOAP()
        require(s.isNotBlank())
        val ga = KSoap.parseSOAP<GetAllAvailableStatusDetailsResponse>(x("/pi_AdapterMM/25statuses.xml"), fault)
        require(ga!!.Response.list.size == 185 && ga.Response.list[0].errorLabelID == "6401")

        s = GetConnections().composeSOAP()
        require(s.isNotBlank())
        val conns = KSoap.parseSOAP<GetConnectionsResponse>(x("/pi_AdapterMM/26conns.xml"), fault)
        require(conns!!.Response.strings.size == 29 && conns.Response.strings[0] == "AFW")

        s = GetErrorCodes(6401).composeSOAP()
        require(s.isNotBlank())
        val ec = KSoap.parseSOAP<GetErrorCodesResponse>(x("/pi_AdapterMM/27error.xml"), fault)
        require(ec!!.Response.strings.size == 1 && ec.Response.strings[0] == "ODATA_INITIALIZATION_ERROR")

        s = GetIntegrationFlows("EN").composeSOAP()
        require(s.isNotBlank())
        val flows = KSoap.parseSOAP<GetIntegrationFlowResponse>(x("/pi_AdapterMM/28flows.xml"), fault)
        require(fault.isSuccess() && flows!!.Response.list.size == 2 && flows.Response.list[0].name == "|SID400|SI_OutSync||")

        s = GetInterfaces().composeSOAP()
        require(s.isNotBlank())
        val iface = KSoap.parseSOAP<GetInterfacesResponse>(x("/pi_AdapterMM/29iface.xml"), fault)
        require(iface!!.Response == null)

        s = GetMessagesWithSuccessors(ArrayOfStrings("7b761582-1227-11ed-8bad-0000004c2912")).composeSOAP()
        require(s.isNotBlank())
        val suc = KSoap.parseSOAP<GetMessagesWithSuccessorsResponse>(x("/pi_AdapterMM/30suc.xml"), fault)
        require(fault.isSuccess() && suc!!.Resp.afw!!.list.size == 1)

        s = GetParties().composeSOAP()
        require(s.isNotBlank())
        val part = KSoap.parseSOAP<GetPartiesResponse>(x("/pi_AdapterMM/31part.xml"), fault)
        require(part!!.Response == null && fault.isSuccess())

        s = GetPredecessorMessageId("7b761582-1227-11ed-8bad-0000004c2912", "OUTBOUND").composeSOAP()
        require(s.isNotBlank())
        val pred = KSoap.parseSOAP<GetPredecessorMessageIdResponse>(x("/pi_AdapterMM/32pred.xml"), fault)
        require(pred!!.Response == null && fault.isSuccess())

        s = GetServices().composeSOAP()
        require(s.isNotBlank())
        val serv = KSoap.parseSOAP<GetServicesResponse>(x("/pi_AdapterMM/33serv.xml"), fault)
        require(serv!!.Response == null && fault.isSuccess())

        s = GetStatusDetails(ArrayOfStrings("ODATA_INITIALIZATION_ERROR"), LocaleAMM("en")).composeSOAP()
        require(s.isNotBlank())
        val stat = KSoap.parseSOAP<GetStatusDetailsResponse>(x("/pi_AdapterMM/34stat.xml"), fault)
        require(fault.isSuccess() && stat!!.Response.list.size == 1 && stat.Response.list[0].errorLabelID == "6401")
    }

//    @Test
//    fun mime() {
//        val ds = ByteArrayDataSource(x("/pi_AdapterMM/mime.txt"), "multipart/related")
//        val mm = MimeMultipart(ds)
//        var i = 0
//        while (i < mm.count) {
//            val bp = mm.getBodyPart(i)
//            for (hd in bp.allHeaders) {
//                require(hd is Header)
//                println("${hd.name}\t${hd.value}")
//            }
//            println(bp.inputStream.readAllBytes().size)
//            i++
//        }
//        require(i == 2)
//    }

}