package no.nav.syfo.dialogporten.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClientException
import no.nav.syfo.dialogporten.domain.Content
import no.nav.syfo.dialogporten.domain.CreateDialogRequest
import no.nav.syfo.dialogporten.domain.Dialog
import no.nav.syfo.dialogporten.domain.create
import no.nav.syfo.texas.client.TexasHttpClient
import no.nav.syfo.util.logger
import java.util.UUID

class DialogportenClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val texasHttpClient: TexasHttpClient,
) {
    private val dialogportenUrl = "$baseUrl/dialogporten/api/v1/serviceowner/dialogs"
    private val logger = logger()

    suspend fun createDialog(createDialogRequest: CreateDialogRequest, ressurs: String): UUID {
        val texasResponse = texasHttpClient.systemToken("maskinporten", "digdir:dialogporten.serviceprovider")
        val token = altinnExchange(texasResponse.accessToken)

        val dialog =
            buildDialogFromRequest(createDialogRequest, ressurs)
        return runCatching<DialogportenClient, UUID> {
            val response =
                httpClient
                    .post(dialogportenUrl) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        header(HttpHeaders.Accept, ContentType.Application.Json)
                        bearerAuth(token)

                        setBody(dialog)
                    }.body<String>()
            UUID.fromString(response.removeSurrounding("\""))
        }.getOrElse { e ->
            logger.error("Feil ved kall til Dialogporten for å opprette dialog", e)
            throw DialogportenClientException(e.message ?: "Feil ved kall til Dialogporten")
        }
    }

    private fun buildDialogFromRequest(createDialogRequest: CreateDialogRequest, ressurs: String): Dialog =
        Dialog(
            serviceResource = "urn:altinn:resource:$ressurs",
            party = "urn:altinn:organization:identifier-no:${createDialogRequest.orgnr}",
            externalReference = createDialogRequest.externalReference,
            content =
                Content.create(
                    title =
                        createDialogRequest.title,
                    summary =
                        createDialogRequest.summary,
                ),
            transmissions = createDialogRequest.transmissions,
            isApiOnly = createDialogRequest.isApiOnly,
        )

    private suspend fun altinnExchange(token: String): String =
        httpClient
            .get("$baseUrl/authentication/api/v1/exchange/maskinporten") {
                bearerAuth(token)
            }.bodyAsText()
            .replace("\"", "")
}
