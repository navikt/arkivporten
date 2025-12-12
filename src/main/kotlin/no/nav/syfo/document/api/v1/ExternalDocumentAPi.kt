package no.nav.syfo.document.api.v1

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.Instant
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.SystemPrincipal
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.texas.MaskinportenIdportenAndTokenXAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

const val DOCUMENT_API_PATH = "/documents"

fun Route.registerExternalDocumentsApiV1(
    DocumentDAO: DocumentDAO,
    texasHttpClient: TexasHttpClient,
    validationService: ValidationService
) {
    route("$DOCUMENT_API_PATH/{id}") {

        install(MaskinportenIdportenAndTokenXAuthPlugin) {
            client = texasHttpClient
        }
        get() {
            val linkId = call.parameters.extractAndValidateUUIDParameter("id")
            val principal = call.getPrincipal()
            val documentDAO = DocumentDAO.getByLinkId(linkId) ?: throw NotFoundException("Document not found")
            validationService.validateDocumentAccess(principal, documentDAO)
            if (!documentDAO.isRead) {
                DocumentDAO.update(documentDAO.copy(isRead = true, updated = Instant.now()))
            }
            call.response.headers.append(HttpHeaders.ContentType, documentDAO.contentType)
            call.respond<ByteArray>(documentDAO.content)
            countRead(principal, documentDAO.isRead)
            call.response.status(HttpStatusCode.OK)
        }
    }

}

fun countRead(principal: no.nav.syfo.application.auth.Principal, isRead: Boolean) {
    if (isRead) {
        when (principal) {
            is BrukerPrincipal -> COUNT_DOCUMENTS_REREAD_BY_EXTERNAL_IDPORTENUSER.increment()
            is SystemPrincipal -> COUNT_DOCUMENTS_REREAD_BY_EXTERNAL_SYSTEMUSER.increment()
        }
    } else {
        when (principal) {
            is BrukerPrincipal -> COUNT_DOCUMENTS_READ_BY_EXTERNAL_IDPORTENUSER.increment()
            is SystemPrincipal -> COUNT_DOCUMENTS_READ_BY_EXTERNAL_SYSTEMUSER.increment()
        }
    }
}
