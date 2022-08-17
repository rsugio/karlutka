import KT.Companion.s
import karlutka.parsers.pi.SAPControl
import karlutka.serialization.KSoap
import kotlin.test.Test

class KSAPControlTest {
    @Test
    fun v1() {
        val fault = KSoap.Fault()
        require(SAPControl.ListLogFiles().composeSOAP().isNotBlank())
        val llfr = SAPControl.ListLogFilesResponse.parseSOAP(s("pi_SAPControl/01listlogfiles.xml"), fault)
        require(fault.isSuccess() && llfr!!.file.item.size == 755)
        val rlf = SAPControl.ReadLogFileResponse.parseSOAP(s("pi_SAPControl/02readlogfile.xml"), fault)
        require(fault.isSuccess())
        println(rlf)
        val e = SAPControl.ReadLogFileResponse.parseSOAP(s("pi_SAPControl/03readlogfile_fault.xml"), fault)
        require(fault.isFailure() && e == null)
        println(fault)
    }
}