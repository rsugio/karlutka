import KT.Companion.s
import karlutka.parsers.pi.SAPControl
import karlutka.serialization.KSoap
import kotlin.test.Test

class KSAPControlTest {
    @Test
    fun v1() {
        val fault = KSoap.Fault()
        require(SAPControl.ListLogFiles().composeSOAP().isNotBlank())
        val llfr = KSoap.parseSOAP<SAPControl.ListLogFilesResponse>(s("/pi_SAPControl/01listlogfiles.xml"), fault)
        require(fault.isSuccess() && llfr!!.file.item.size == 755)
        val rlf = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/02readlogfile.xml"), fault)
        require(fault.isSuccess() && rlf!!.format.startsWith("Version"))
        val e = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/03readlogfile_fault.xml"), fault)
        require(!fault.isSuccess() && fault.faultstring == "Invalid filename" && e == null)
    }
}