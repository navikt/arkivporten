package no.nav.syfo.narmesteleder.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.JwtIssuer
import no.nav.syfo.application.auth.OrganisasjonPrincipal
import no.nav.syfo.application.auth.Principal
import no.nav.syfo.application.auth.TOKEN_ISSUER
import no.nav.syfo.application.exceptions.UnauthorizedException
import no.nav.syfo.texas.MaskinportenAndTokenXTokenAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

fun Route.registerNarmestelederApiV1(
    texasHttpClient: TexasHttpClient,
) {
    route("/narmesteleder") {
        install(MaskinportenAndTokenXTokenAuthPlugin) {
            client = texasHttpClient
        }

        post() {
            val nlRelasjon = call.tryReceive<NarmesteLederRelasjonerWrite>()

            call.respond(HttpStatusCode.Accepted)
        }
    }

    route("/narmesteleder/avkreft") {
        post() {
            val avkreft = call.tryReceive<NarmestelederRelasjonAvkreft>()
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

fun RoutingCall.getMyPrincipal(): Principal =
    when (attributes[TOKEN_ISSUER]) {
        JwtIssuer.MASKINPORTEN -> {
            authentication.principal<OrganisasjonPrincipal>() ?: throw UnauthorizedException()
        }

        JwtIssuer.TOKEN_X -> {
            authentication.principal<BrukerPrincipal>() ?: throw UnauthorizedException()
        }

        else -> throw UnauthorizedException()
    }
