package ru.rsug.karlutka.util

import java.io.InputStream
import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.io.path.inputStream

object KKeystore {
    val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    fun load(keyStorePath: Path, passwd: CharArray) {
        load(keyStorePath.inputStream(), passwd)
    }

    fun load(inputStream: InputStream, passwd: CharArray) {
        keyStore.load(inputStream, passwd)
    }

    fun getTrustManagerFactory(): TrustManagerFactory? {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        return trustManagerFactory
    }

    fun getSslContext(): SSLContext? {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, getTrustManagerFactory()?.trustManagers, null)
        return sslContext
    }
}
