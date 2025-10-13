package no.nav.syfo

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AddTokenIssuerPlugin
import no.nav.syfo.assets.api.v1.registerAssetsApiV1
import no.nav.syfo.narmesteleder.api.v1.registerNarmestelederApiV1
import no.nav.syfo.texas.TexasAzureADAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

@Suppress("LongParameterList")
fun Route.registerApiV1(
    texasHttpClient: TexasHttpClient,
) {
    route("/api/v1") {
        install(AddTokenIssuerPlugin)
        registerNarmestelederApiV1(texasHttpClient)
    }
    route("/internal/api/v1") {
        install(TexasAzureADAuthPlugin) {
            client = texasHttpClient
        }
        registerAssetsApiV1(texasHttpClient)
    }

}
