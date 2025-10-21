package no.nav.syfo.dialogporten

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AddTokenIssuerPlugin
import no.nav.syfo.dialogporten.client.DialogportenClient
import no.nav.syfo.texas.MaskinportenIdportenAndTokenXAuthPlugin

fun Route.registerDialogportenTokenApi(
    dialogportenClient: DialogportenClient,
) {
    route("/dialogporten/token") {
        install(AddTokenIssuerPlugin)
        get {
            install(MaskinportenIdportenAndTokenXAuthPlugin)

            call.respondText(dialogportenClient.getDialogportenToken())
        }
    }
}
