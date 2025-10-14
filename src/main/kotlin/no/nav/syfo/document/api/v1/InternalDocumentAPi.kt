package no.nav.syfo.document.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.document.db.DocumentDb
import no.nav.syfo.narmesteleder.api.v1.tryReceive
import no.nav.syfo.util.logger

fun Route.registerAssetsApiV1(
    documentDb: DocumentDb,
) {
    route("/documents") {

        post() {
            val asset = call.tryReceive<Document>()
            runCatching {
                documentDb.insert(asset.toDocumentDAO())
                call.respond(HttpStatusCode.OK)
            }.onFailure {
                logger().error("Failed to insert document: ${it.message}", it)
                throw ApiErrorException.InternalServerErrorException("Failed to insert document")
            }
        }
    }
}
