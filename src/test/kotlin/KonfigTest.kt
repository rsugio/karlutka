import KT.Companion.s
import karlutka.util.KfPasswds
import karlutka.util.KfTarget
import karlutka.util.Kfg
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class KonfigTest {
    fun show(a: Any) {
        if (true) println(a)
    }

    @Test
    fun karla() {
        val k0 = Kfg.parse(s("Kfg/empty.yaml"))
        assertEquals(0, k0.targets.size)

        val rus = s("Kfg/karla.yaml")
        require(rus.contains("Русские буквы"))
        val k1 = Kfg.parse(rus)
        show(k1)
        require(k1.targets.size == 4)
        val a1 = k1.targets[0] as KfTarget.ABAP
        assertEquals("разработка", a1.text)
        show(a1.jco.toProperties())
        val p1 = k1.targets[1] as KfTarget.PIAF
        require(p1.text == null)
        val btpneo = k1.targets[2] as KfTarget.BTPNEO
        require(btpneo.apihost == "https://api.eu3.hana.ondemand.com")
        val btpcf = k1.targets[3] as KfTarget.BTPCF
        require(btpcf.subaccount == "eddd1d16-2a25-4055-86c0-7405b88ea57d")

        val pws = KfPasswds.parse(s("Kfg/passwd.yaml"))
        k1.targets.forEach { t ->
            t.loadAuths(pws.securityMaterials)
        }
        require(btpneo.oauth.client_id == "12323232-2323-2323-9233-000000000000")
        require(btpcf.oauth.client_id == "sb-na-bfb08b56-80b0-4182-8a6e-530ecdfb5772!a93363")
    }

    @Test
    fun prints() {
        val t1 = KfTarget.ABAP(
            "TER", "руссъ язь",
            mutableMapOf("jco.client.ashost" to "host", "jco.client.client" to "400")
        )
        val t2 = KfTarget.PIAF("QPH", "ЪЩьЬ", "https://host.local.ondemand.com:44339")
        val t3 = KfTarget.BTPNEO("azc20mq", null, "global", "https://api.eu3.hana.ondemand.com", "?", "?")
        require(t3.globalaccount == "global")
//        val t4 = Target.CPINEO("e10001", "https://e10001.tmn.eu3.hana.ondemand.com")
        val t5 = KfTarget.BTPCF(
            "bc09a195trial",
            "us10",
            "bc09a195trial",
            "eddd1d16-2a25-4055-86c0-7405b88ea57d",
            "https://api.cf.us10.hana.ondemand.com",
            "?"
        )
//        val t6 = Target.CPICF("e10001", "https://e10001.tmn.eu3.hana.ondemand.com")

        val k1 = Kfg(mutableListOf(t1, t2, t3, t5))

        val rus = k1.encodeToString()
        show(rus)
        require(k1.encodeToString().contains("руссъ язь"))
    }

    @Test
    fun passwds() {
        val kfp = KfPasswds.parse(s("Kfg/passwd.yaml"))
        require(kfp.securityMaterials.size == 6, { kfp.securityMaterials.size })
        //TODO добавить проверку загрузки кейстора
        require(kfp.keystore.path == Paths.get("keystore.jks"))
        require(kfp.keystore.passwd.contentEquals("".toCharArray()))
        show(kfp.encodeToString())
    }

    @Test
    fun live() {
        val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
        val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))

    }
}
