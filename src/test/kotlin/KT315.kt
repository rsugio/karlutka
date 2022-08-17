import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import kotlin.test.Test


class KT315 {
    val client_id = "65ed6bda-ff54-3d63-972f-ddeb609f35d8"
    val client_secret = "ca632e79-bed8-3a51-a7fb-5b9960e580a0"
    val token = "https://api.eu3.hana.ondemand.com/oauth2/apitoken/v1?grant_type=client_credentials"
    val subaccount = "jpwnc20mwh"
    val apps = listOf("e500230tmn", "e500230iflmap", "provision")
    val providerAccount = "avteu3cpie"

    fun token() {
        val client = HttpClient.newBuilder()
            .authenticator(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(client_id, client_secret.toCharArray())
                }
            })
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(token))
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val rc = response.statusCode()
        println(rc)
        println(response.body())
    }

    @Test
    fun v() {
        //println("v")
        //KT315().token()
        println(Instant.now().toEpochMilli() * 1000000)
        println(Instant.ofEpochMilli(1660667658610000000 / 1000000))
    }
}