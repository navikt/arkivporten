package no.nav.syfo.pdp.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.syfo.application.exception.UpstreamRequestException

class PdpClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val subscriptionKey: String,
) {
    suspend fun authorize(
        bruker: Bruker,
        orgnrSet: Set<String>,
        ressurs: String
    ): PdpResponse {
        val request = lagPdpRequest(bruker, orgnrSet, ressurs)
        val response = try {
            httpClient
                .post("$baseUrl/authorization/api/v1/authorize") {
                    header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    header("Content-Type", "application/json")
                    header("Accept", "application/json")
                    setBody(request)
                }
                .body<PdpResponse>()
        } catch (e: ResponseException) {
            throw UpstreamRequestException("Error while calling PDP", e)
        }
        return response
    }
}
