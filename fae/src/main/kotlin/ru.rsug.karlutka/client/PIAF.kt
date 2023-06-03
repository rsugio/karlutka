package ru.rsug.karlutka.client

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import ru.rsug.karlutka.pi.XICache
import ru.rsug.karlutka.sld.Cim
import ru.rsug.karlutka.sld.SLD_CIM
import ru.rsug.karlutka.util.DB
import ru.rsug.karlutka.util.Konfig
import ru.rsug.karlutka.util.KtorClient
import java.net.URL
import java.util.*

class PIAF(val konfig: Konfig.Target.PIAF) {
    val httpHostPort: URL
    val client: HttpClient
    private val hmiClientId: UUID = UUID.randomUUID()!!
    val afs = mutableListOf<String>()
    private val contextuser = "_"                   // пользователь, использующийся в контекстных запросах

    init {
        httpHostPort = URL(konfig.url)
        client = KtorClient.createClient(konfig.url, konfig.retries, konfig.logLevel)
        requireNotNull(konfig.basic)
        KtorClient.setBasicAuth(client, konfig.basic!!.login, konfig.basic!!.passwd(), true)
    }

    fun urlOf(res: String = ""): URL {
        return if (res.isEmpty())
            httpHostPort
        else
            httpHostPort.toURI().resolve(res).toURL()
    }

    suspend fun pingNoAuth(): String {
        val resp = client.get("/")

        require(resp.status.isSuccess())
        return resp.headers["server"].toString()
    }

    /**
     * Пока смотрим, чтобы ответ был предсказуемым
     */
    suspend fun checkAuth(resource: String, expected: String? = null) {
        val resp = client.get(resource)
        require(resp.status.isSuccess()) { "Cannot login to ${httpHostPort} path=${resource}: response code ${resp.status}" }
        val resptext = resp.bodyAsText()
        require(!resptext.contains("logon_ui_resources")) { "Cannot login to ${httpHostPort} path=${resource}: PASSWORD incorrect. '$resptext'" }
        if (expected != null) require(resptext.contains(expected), { resptext })
    }

    // подумать, может быть в HmiResponse добавить ссылку на Task и здесь её заполнять
    // это удобно чтобы не удалять файл после успешного разбора
    private suspend fun taskAwait(td: Deferred<KtorClient.Task>, expected: ContentType): KtorClient.Task {
        val task = td.await()
        while (task.retries < 10) {
            requireNotNull(task.resp) {"Метод execute не был вызван"}
            if (task.resp.status.isSuccess() && task.resp.contentType()!!.match(expected)) {
                return task
            }
            task.execute()
        }
        error("HMI POST - ошибка после 10 повторов")
    }

    suspend fun valueMappingCache(queryParams: String) {
        val res = "/run/value_mapping_cache/ext?$queryParams"
        val iv = KtorClient.taskGet(client, res)
        iv.execute()
        require(iv.isXml() && iv.resp.status.isSuccess())
    }

    suspend fun dirHmiCacheRefreshService(mode: String, consumer: String): XICache.CacheRefresh {
        // /dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=C&consumer=af.fa0.fake0db
        val iv = KtorClient.taskGet(client, "/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=$mode&consumer=$consumer")
        iv.execute()
        require(iv.isXml() && iv.resp.status.isSuccess())
        return XICache.decodeCacheRefreshFromReader(iv.bodyAsXmlReader())
    }
}
