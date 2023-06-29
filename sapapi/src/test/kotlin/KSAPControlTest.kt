import KT.Companion.s
import org.junit.jupiter.api.Tag
import ru.rsug.karlutka.pi.SAPControl
import ru.rsug.karlutka.serialization.KSoap
import kotlin.test.Test

@Tag("Offline")
class KSAPControlTest {
    val fault = KSoap.Fault()

    @Test
    fun logs() {
        require(SAPControl.ListLogFilesRequest().composeSOAP().isNotBlank())
        val llfr = KSoap.parseSOAP<SAPControl.ListLogFilesResponse>(s("/pi_SAPControl/01ListLogFilesResponse.xml"), fault)
        require(fault.isSuccess() && llfr!!.file.item.size == 755)
        val rlf = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/02ReadLogFileResponse.xml"), fault)
        require(fault.isSuccess() && rlf!!.format.startsWith("Version"))
        val e = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/03Fault.xml"), fault)
        require(!fault.isSuccess() && fault.faultstring == "Invalid filename" && e == null)
    }

    @Test
    fun thread() {
        KSoap.parseSOAP<SAPControl.J2EEGetThreadListResponse>(s("/pi_SAPControl/07J2EEGetThreadListResponse.xml"), fault)
        KSoap.parseSOAP<SAPControl.J2EEGetThreadList2Response>(s("/pi_SAPControl/08J2EEGetThreadList2Response.xml"), fault)
    }

    @Test
    fun devtraces() {
        KSoap.parseSOAP<SAPControl.ListDeveloperTracesRequest>(s("/pi_SAPControl/04ListDeveloperTracesResponse.xml"), fault)
        KSoap.parseSOAP<SAPControl.ReadDeveloperTraceRequest>(s("/pi_SAPControl/05ReadDeveloperTraceRequest.xml"), fault)
        KSoap.parseSOAP<SAPControl.ReadDeveloperTraceResponse>(s("/pi_SAPControl/06ReadDeveloperTraceResponse.xml"), fault)
    }
}