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
import no.nav.syfo.util.logger
import org.slf4j.Logger

const val DOCUMENT_API_PATH = "/documents"

fun Route.registerExternalDocumentsApiV1(
    DocumentDAO: DocumentDAO,
    texasHttpClient: TexasHttpClient,
    validationService: ValidationService
) {
    val logger = logger("ExternalDocumentAPi")
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
            countRead(logger, principal, documentDAO.isRead, documentDAO.dialog.orgNumber)
            call.response.status(HttpStatusCode.OK)
        }
    }

}

fun countRead(
    logger: Logger,
    principal: no.nav.syfo.application.auth.Principal,
    isRead: Boolean,
    orgNumber: String,
) {
    if (isRead) {
        when (principal) {
            is BrukerPrincipal -> {
                COUNT_DOCUMENTS_REREAD_BY_EXTERNAL_IDPORTENUSER.increment()
                logger.warn("Document belonging to orgNUmber $orgNumber was idporten user")
            }

            is SystemPrincipal -> {
                COUNT_DOCUMENTS_REREAD_BY_EXTERNAL_SYSTEMUSER.increment()
                logger.warn("Document belonging to orgNUmber $orgNumber was system user with owner ${principal.systemOwner}")
            }
        }
    } else {
        when (principal) {
            is BrukerPrincipal -> COUNT_DOCUMENTS_READ_BY_EXTERNAL_IDPORTENUSER.increment()
            is SystemPrincipal -> COUNT_DOCUMENTS_READ_BY_EXTERNAL_SYSTEMUSER.increment()
        }
    }
}
