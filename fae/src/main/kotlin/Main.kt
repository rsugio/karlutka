import com.sap.conn.jco.JCo
import ru.rsug.karlutka.client.PIAF
import ru.rsug.karlutka.client.SLD
import ru.rsug.karlutka.server.FAE
import ru.rsug.karlutka.server.KtorServer
import ru.rsug.karlutka.server.Server
import ru.rsug.karlutka.util.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

suspend fun main(args: Array<String>) {
    val pid = ProcessHandle.current().pid()
    println("[ru-hello]Привет (∀x∈X)P(x), pid=$pid")

    val pkfg: Path = if (args.isEmpty()) {
        Paths.get("karla.yaml")
    } else {
        Paths.get(args[0])
    }
    val ppw: Path = if (args.size < 2) {
        Paths.get("passwd.yaml")
    } else {
        Paths.get(args[1])
    }
    println("Конфиг=${pkfg.toAbsolutePath()}, пароли=${ppw.toAbsolutePath()}")
    // Эта проверка инициализирует JCo, проверяет наличие отдельного sapjco3.jar
    // и работоспособность sapjco3.dll под текущую операционную систему
    require(JCo.getDestinationIDs().isEmpty())
    println("JCo под текущую архитектуру инициализирован и работает")

    val kfg = Konfig.Kfg.parse(pkfg)
    val pw = Konfig.KfPasswds.parse(ppw)
    KKeystore.load(pw.keystore.path, pw.keystore.passwd)

    KtorServer.createServer(kfg.httpServerListenPort, kfg.httpServerListenAddress)
    // println("ktor.application.developmentMode: ${KTorUtils.server.application.developmentMode}")
    // прописывать свойства Server можно лишь только после создания сервера, если включен development mode
    // иначе синглтон будет инициализирован заново
    Server.kfg = kfg
    Server.kfpasswds = pw
    Server.pkfg = pkfg
    Server.ppw = ppw

    KTempFile.tempFolder = Paths.get(kfg.tmpdir)
    KTempFile.start()
    println("Вре́менные файлы в ${KTempFile.tempFolder}")
    DB.init(kfg.h2connection)
    KtorClient.createClientEngine(4, Duration.ofMillis(2000))

    println("Загружаем соединения")
    Server.kfg.targets.forEach { konf ->
        konf.loadAuths(Server.kfpasswds.securityMaterials)
        val t : Any = when (konf) {
            is Konfig.Target.PIAF -> {
                val pi = PIAF(konf)
                if (konf.checkAuthResource.isNotEmpty()) {
                    pi.checkAuth(konf.checkAuthResource)
                }
                pi
            }
            is Konfig.Target.FAE -> {
                require(Server.targets.values.filterIsInstance<FAE>().isEmpty())
                Server.fae = FAE(konf, Server.targets[konf.cae]!! as PIAF, SLD(Server.targets[konf.sld]!! as PIAF))
                Server.fae!!
            }
//            is Konfig.Target.ABAP -> target = AbapJCo(konf)
//            is KfTarget.BTPNEO -> target = BTPNEO(konf)
//            is KfTarget.BTPCF -> target = BTPCF(konf)
//            is KfTarget.CPINEO -> target = CPINEO(konf)
            else -> error("Not implemented target: $konf")
        }
        Server.targets[konf.sid] = t
        println("\tзагружен ${konf.sid}")
    }

    println("Протяжка продувка .. Ktor-server готовится к запуску на ${kfg.httpServerListenAddress}:${kfg.httpServerListenPort}")
    KtorServer.server.start(wait = false)
    if (Server.fae!=null) {
        println("Работаем в режиме FAE")
        Server.fae!!.ktor(KtorServer.app)   // до старта добавить маршруты нельзя
    } else {
        println("FAE отключен")
    }
    println("ПОЕХАЛИ!")
    KtorServer.server.start(wait = true)
    // здесь больше ничего писать нельзя - dead code
}
