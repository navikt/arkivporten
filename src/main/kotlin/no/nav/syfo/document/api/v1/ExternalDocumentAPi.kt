package no.nav.syfo.document.api.v1

import io.github.tabilzad.ktor.annotations.GenerateOpenApi
import io.github.tabilzad.ktor.annotations.KtorDescription
import io.github.tabilzad.ktor.annotations.KtorResponds
import io.github.tabilzad.ktor.annotations.ResponseEntry
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.texas.MaskinportenIdportenAndTokenXAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

const val DOCUMENT_API_PATH = "/documents"

@GenerateOpenApi
fun Route.registerExternalDocumentsApiV1(
    DocumentDAO: DocumentDAO,
    texasHttpClient: TexasHttpClient,
    validationService: ValidationService
) {
    route("$DOCUMENT_API_PATH/{id}") {

        install(MaskinportenIdportenAndTokenXAuthPlugin) {
            client = texasHttpClient
        }
        @KtorDescription(tags = ["documents"], summary = "Get document by id", description = "Retrieve a document using its unique identifier.")
        @KtorResponds(
            [
                ResponseEntry("200", String::class, isCollection=true, description = "Get document"),
                ResponseEntry("404", String::class, description = "Document not found"),
                ResponseEntry("500", String::class, description = "Internal server error")
            ],
        )
        get() {
            val linkId = call.parameters.extractAndValidateUUIDParameter("id")
            val principal = call.getPrincipal()
            val documentDAO = DocumentDAO.getByLinkId(linkId) ?: throw NotFoundException("Document not found")
            validationService.validateDocumentAccess(principal, documentDAO)
            call.response.headers.append(HttpHeaders.ContentType, documentDAO.contentType)
            call.respond<ByteArray>(documentDAO.content)
            call.response.status(HttpStatusCode.OK)
        }
    }
}
