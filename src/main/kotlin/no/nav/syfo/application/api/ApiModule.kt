package no.nav.syfo.application.api

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.metric.registerMetricApi
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.ereg.EregService
import no.nav.syfo.registerApiV1
import no.nav.syfo.texas.client.TexasHttpClient
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val applicationState by inject<ApplicationState>()
    val database by inject<DatabaseInterface>()
    val texasHttpClient by inject<TexasHttpClient>()
    val documentDAO by inject<DocumentDAO>()
    val validationService by inject<ValidationService>()

    installCallId()
    installContentNegotiation()
    installStatusPages()

    routing {
        registerPodApi(applicationState, database)
        registerMetricApi()
        registerApiV1(texasHttpClient, documentDAO, validationService)
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
