package no.nav.syfo.application.api

import io.github.tabilzad.ktor.annotations.GenerateOpenApi
import io.github.tabilzad.ktor.annotations.KtorDescription
import io.github.tabilzad.ktor.annotations.KtorResponds
import io.github.tabilzad.ktor.annotations.ResponseEntry
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.isProdEnv
import no.nav.syfo.application.metric.registerMetricApi
import no.nav.syfo.dialogporten.client.IDialogportenClient
import no.nav.syfo.dialogporten.registerDialogportenTokenApi
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.registerApiV1
import no.nav.syfo.texas.client.TexasHttpClient
import org.koin.ktor.ext.inject

@GenerateOpenApi
fun Application.configureRouting() {
    val applicationState by inject<ApplicationState>()
    val database by inject<DatabaseInterface>()
    val texasHttpClient by inject<TexasHttpClient>()
    val documentDAO by inject<DocumentDAO>()
    val validationService by inject<ValidationService>()
    val dialogportenClient by inject<IDialogportenClient>()

    installCallId()
    installContentNegotiation()
    installStatusPages()

    routing {
        registerPodApi(applicationState, database)
        registerMetricApi()
        registerApiV1(texasHttpClient, documentDAO, validationService)
        if (!isProdEnv()) {
            // TODO: Remove this endpoint later
            registerDialogportenTokenApi(texasHttpClient, dialogportenClient)
        }
        @KtorDescription(
            summary = "Hello World endpoint",
            description = "This endpoint will create an order",
        )
        @KtorResponds([ResponseEntry("200", String::class, isCollection=true, description = "Get hello world response")])
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
