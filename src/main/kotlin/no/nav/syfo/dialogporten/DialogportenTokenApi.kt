package no.nav.syfo.dialogporten

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AddTokenIssuerPlugin
import no.nav.syfo.dialogporten.client.IDialogportenClient
import no.nav.syfo.texas.MaskinportenIdportenAndTokenXAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

fun Route.registerDialogportenTokenApi(
    texasHttpClient: TexasHttpClient,
    dialogportenClient: IDialogportenClient,
) {
    route("/dialogporten/token") {
        install(AddTokenIssuerPlugin)
        get {
            install(MaskinportenIdportenAndTokenXAuthPlugin) {
                client = texasHttpClient
            }

            call.respondText(dialogportenClient.getDialogportenToken())
        }
    }
}
