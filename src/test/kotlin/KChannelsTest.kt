import KT.Companion.s
import karlutka.parsers.pi.ChannelAdminServlet
import karlutka.parsers.pi.IChannelAdmin.*
import karlutka.parsers.pi.RemoteApi
import kotlin.test.Test

class KChannelsTest {
    @Test
    fun IChannelAdminTest() {
        //TODO доделать нормальные тесты и в целом забыть. Остаётся здесь на всякий случай.
        val lst = mutableListOf(ChannelAdminDescriptor("CC_Abc_Rest_Sender", "BC_Service", "P_PARTY"))
        val stat = ChannelAdminStatus("errorInfo", "automationStatus")
        lst.add(ChannelAdminDescriptor("CC_Abc_Rest_Recv", "BC_COMP", "", mutableListOf(stat)))
        lst.add(ChannelAdminDescriptor("CC_Abc_Rest_Recv", "", "P_1C"))
        val a = StartChannels(lst, "EN")
        println(a.composeSOAP())

        val b = StopChannels(lst, "RU")
        println(b.composeSOAP())

        val c = GetChannelAdminHistory(lst, "EN")
        println(c.composeSOAP())
    }

    @Test
    fun ChannelAdminServletTest() {
        val ei = ChannelAdminServlet.ErrorInformationType(null, null, "")
        val err00 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/00error.xml"), ei)
        require(ei.isFailure() && err00.isEmpty())
        require(ei.usage == "http://h:50000/AdapterFramework/ChannelAdminServlet?action={start|stop|status}&party=<party>&service=<service>&channel=<channelName>[&showProcessLog={true|false}][&showAdminHistory={true|false}][&status={all|ok|error|stopped|inactive|unknown|unregistered}]")
        val a01 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/01list.xml"), ei)
        require(ei.isSuccess() && a01.size == 2)
        val a02 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/02list.xml"), ei)
        require(ei.isSuccess() && a02.size == 3 && a02[2].AdminHistory != null)
        require(a02[2].AdminHistory!!.adminEntries.size > 1)
        require(a02[2].AdminHistory!!.adminEntries[1].AdminErrors!!.errorEntries[0].Time == "1")
        // неуспешный стоп
        val s03 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/03stop.xml"), ei)
        require(ei.isSuccess() && s03[0].AdminErrorInformation!!.startsWith("com.sap.aii.af.service.administration.impl.WrongAutomationModeException"))
        // успешный стоп
        val s04 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/04stop.xml"), ei)
        require(ei.isSuccess() && s04[0].ShortLog == "Server 00 00_49912 : Channel stopped")
        // успешный старт
        val s05 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/05start.xml"), ei)
        require(ei.isSuccess() && s05[0].AdminErrorInformation == null)
        // ошибочный старт
        val s06 = ChannelAdminServlet.Companion.parse(s("pi_ChannelAdmin/06start.xml"), ei)
        require(ei.isSuccess() && s06[0].AdminErrorInformation != null)
        //
    }

    @Test
    fun RemoteApiTest() {
        val a1 = RemoteApi.Scenario.parse(s("pi_RemoteApi/01stopped.xml"))
        println(a1)
        val a2 = RemoteApi.Scenario.parse(s("pi_RemoteApi/02ok.xml"))
        println(a2)
        val a3 = RemoteApi.Scenario.parse(s("pi_RemoteApi/03inactive.xml"))
        println(a3)
        val a4 = RemoteApi.Scenario.parse(s("pi_RemoteApi/04idocerror.xml"))
        println(a4)
    }
}