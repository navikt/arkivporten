package no.nav.syfo.document.api.v1

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AzureAdPrincipal
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.document.api.v1.dto.Document
import no.nav.syfo.document.api.v1.dto.DocumentType.*
import no.nav.syfo.document.db.DialogDAO
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.util.logger
import java.util.UUID

fun Route.registerInternalDocumentsApiV1(
    documentDAO: DocumentDAO,
    dialogDAO: DialogDAO
) {
    route("/documents") {
        post() {
            val document = call.tryReceive<Document>()
            runCatching {
                val existingDialog = dialogDAO.getByFnrAndOrgNumber(document.fnr, document.orgNumber)
                    ?: dialogDAO.insertDialog(document.toDialogEntity())
                documentDAO.insert(document.toDocumentEntity(existingDialog))
                COUNT_DOCUMENT_RECIEVED.increment()
                call.respond(HttpStatusCode.OK)
            }.onFailure {
                logger().error("Failed to insert document: ${it.message}", it)
                throw ApiErrorException.InternalServerErrorException("Failed to insert document")
            }
        }

        get("/{documentUuid}") {
            val documentUuid = call.parameters["documentUuid"]
                ?: throw ApiErrorException.BadRequestException("Missing documentUuid parameter")

            val document = documentDAO.getByDocumentId(UUID.fromString(documentUuid))
                ?: throw ApiErrorException.NotFoundException("Document not found")

            val principal = call.principal<AzureAdPrincipal>()
                ?: throw ApiErrorException.UnauthorizedException("No principal found in call")

            if (!hasAccessToDocument(document, principal)) {
                throw ApiErrorException
                    .ForbiddenException("Caller does not have access to document of type ${document.type}")
            }

            call.response.status(HttpStatusCode.OK)
            call.response.headers.append(HttpHeaders.ContentType, document.contentType)
            call.respond<ByteArray>(document.content)
        }
    }
}

private fun hasAccessToDocument(
    document: DocumentEntity,
    principal: AzureAdPrincipal,
): Boolean {
    return when (document.type) {
        OPPFOLGINGSPLAN -> principal.clientId.contains("syfo-oppfolgingsplan-backend")
        DIALOGMOTE,
        UNDEFINED -> false
    }
}
