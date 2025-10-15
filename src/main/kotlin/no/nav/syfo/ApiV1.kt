package no.nav.syfo

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.syfo.application.auth.AddTokenIssuerPlugin
import no.nav.syfo.document.api.v1.registerExternalDocumentsApiV1
import no.nav.syfo.document.api.v1.registerInternalDocumentsApiV1
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.texas.TexasAzureADAuthPlugin
import no.nav.syfo.texas.client.TexasHttpClient

@Suppress("LongParameterList")
fun Route.registerApiV1(
    texasHttpClient: TexasHttpClient,
    DocumentDAO: DocumentDAO,
    validationService: ValidationService,
) {
    route("/internal/api/v1") {
        install(TexasAzureADAuthPlugin) {
            client = texasHttpClient
        }
        registerInternalDocumentsApiV1(DocumentDAO)
    }
    route("/api/v1") {
        install(AddTokenIssuerPlugin)
        registerExternalDocumentsApiV1(DocumentDAO, texasHttpClient, validationService)
    }

}
