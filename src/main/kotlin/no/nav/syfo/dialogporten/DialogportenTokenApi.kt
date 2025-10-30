package no.nav.syfo.dialogporten

import io.github.tabilzad.ktor.annotations.GenerateOpenApi
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AddTokenIssuerPlugin
import no.nav.syfo.dialogporten.client.IDialogportenClient
import no.nav.syfo.texas.MaskinportenIdportenAndTokenXAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

@GenerateOpenApi
fun Route.registerDialogportenTokenApi(
    texasHttpClient: TexasHttpClient,
    dialogportenClient: IDialogportenClient,
) {
    route("/dialogporten/token") {
        install(AddTokenIssuerPlugin)
        install(MaskinportenIdportenAndTokenXAuthPlugin) {
            client = texasHttpClient
        }
        get {
            call.respondText(dialogportenClient.getDialogportenToken())
        }
    }
}
