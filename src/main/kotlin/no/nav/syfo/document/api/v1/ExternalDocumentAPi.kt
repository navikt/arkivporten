package no.nav.syfo.document.api.v1

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.texas.MaskinportenAndTokenXTokenAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

fun Route.registerExternalDocumentsApiV1(
    DocumentDAO: DocumentDAO,
    texasHttpClient: TexasHttpClient,
    validationService: ValidationService
) {
    route("/documents/{id}") {

        install(MaskinportenAndTokenXTokenAuthPlugin) {
            client = texasHttpClient
        }
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
