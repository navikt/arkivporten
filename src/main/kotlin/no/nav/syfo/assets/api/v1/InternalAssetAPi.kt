package no.nav.syfo.assets.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.assets.db.DocumentDb
import no.nav.syfo.narmesteleder.api.v1.tryReceive
import no.nav.syfo.util.logger

fun Route.registerAssetsApiV1(
    documentDb: DocumentDb,
) {
    route("/assets") {

        post() {
            val asset = call.tryReceive<Asset>()
            logger().info("Received asset: $asset")
            documentDb.insert(asset.toDocumentDAO())

            call.respond(HttpStatusCode.OK)
        }
    }
}
