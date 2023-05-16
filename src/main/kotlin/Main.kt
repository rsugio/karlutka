import com.sap.conn.jco.JCo
import karlutka.clients.*
import karlutka.models.MTarget
import karlutka.server.DB
import karlutka.server.SPROXY
import karlutka.server.Server
import karlutka.util.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import kotlin.io.path.inputStream

suspend fun main(args: Array<String>) {
    val pid = ProcessHandle.current().pid()
    println("[ru-hello]Привет (∀x∈X)P(x), pid=$pid")

    val pkfg: Path
    val ppw: Path
    if (args.isEmpty()) {
        pkfg = Paths.get("karla.yaml")
    } else {
        pkfg = Paths.get(args[0])
    }
    if (args.size < 2) {
        ppw = Paths.get("passwd.yaml")
    } else {
        ppw = Paths.get(args[1])
    }
    println("Конфиг=${pkfg.toAbsolutePath()}, пароли=${ppw.toAbsolutePath()}")
    // Эта проверка инициализирует JCo, проверяет наличие отдельного sapjco3.jar
    // и работоспособность sapjco3.dll под текущую операционную систему
    require(JCo.getDestinationIDs().size == 0)
    println("JCo под текущую архитектуру инициализирован и работает")

    val kfg = Kfg.parse(pkfg)
    val pw = KfPasswds.parse(ppw)
    KKeystore.load(pw.keystore.path, pw.keystore.passwd)

    KtorServer.createServer(kfg.httpServerListenPort, kfg.httpServerListenAddress)
    // println("ktor.application.developmentMode: ${KTorUtils.server.application.developmentMode}")
    // прописывать свойства Server можно лишь только после создания сервера, если включен development mode
    // иначе синглтон будет инициализирован заново
    Server.kfg = kfg
    Server.kfpasswds = pw
    Server.pkfg = pkfg
    Server.ppw = ppw
    //TODO - временно{
    val afprops = Properties()
    afprops.load(Paths.get(".etc/af.properties").inputStream())
    Server.afprops = afprops.toMap() as Map<String, String>
    SPROXY.load(Paths.get("c:/data/SPROXY"))
    //}

    KTempFile.tempFolder = Paths.get(kfg.tmpdir)
    KTempFile.start()
    println("Вре́менные файлы в ${KTempFile.tempFolder}")

    KtorClient.createClientEngine(
        Server.kfg.httpClientThreads,
        Duration.ofMillis(Server.kfg.httpClientConnectionTimeoutMillis)
    )

    DB.init(kfg.h2connection)
    if (kfg.influxdb != null && false) {
        val info = KInflux.init(kfg.influxdb, pw.securityMaterials)
        println("Influx по ${kfg.influxdb.host} подключен: $info")
    }

    println("Загружаем соединения")
    Server.kfg.targets.forEach { konf ->
        konf.loadAuths(Server.kfpasswds.securityMaterials)
        val target: MTarget
        var pingAuth = false
        when (konf) {
            is KfTarget.ABAP   -> target = AbapJCo(konf)
            is KfTarget.PIAF   -> {
                val pi = PI(konf)
                if (konf.checkAuthResource.isNotEmpty()) {
                    pi.checkAuth(konf.checkAuthResource)
                    pingAuth = true
                }
                target = pi
            }
            is KfTarget.BTPNEO -> target = BTPNEO(konf)
            is KfTarget.BTPCF  -> target = BTPCF(konf)
            is KfTarget.CPINEO -> target = CPINEO(konf)
        }
        Server.targets[target.getSid()] = target
        if (pingAuth)
            println("\tзагружен ${konf.sid}(${konf.getKind()}), с проверкой доступа")
        else
            println("\tзагружен ${konf.sid}(${konf.getKind()}), без проверки доступа")
    }

    println("Протяжка продувка .. Ktor-server готовится к запуску на ${kfg.httpServerListenAddress}:${kfg.httpServerListenPort}")
    KtorServer.server.start(wait = false)
    println("ПОЕХАЛИ!")
    KtorServer.server.start(wait = true)
    // здесь больше ничего писать нельзя - dead code
}
