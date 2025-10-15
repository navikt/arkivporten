package no.nav.syfo.dialogporten

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.dialogporten.client.DialogportenClient
import no.nav.syfo.dialogporten.domain.CreateDialogRequest
import no.nav.syfo.util.logger

// Kun for test av opprettelse av dialog i dialogporten. Kan slettes senere.
fun Route.registerDialogportenTestApi(
    dialogportenClient: DialogportenClient,
) {
    val logger = logger("DialogportenTestApi")

    route("/dialogporten") {
        post("/test") {
            logger.info("Tester opprettelse av dialog i dialogporten")
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = "310667633", // VENSTRE ROMANTISK TIGER AS
                    title = "Test dialog",
                    summary = "Denne dialogen er opprettet for å teste dialogporten",
                    externalReference = "test-${System.currentTimeMillis()}",
                    isApiOnly = false,
                    transmissions = emptyList(),
                ),
                ressurs = "nav_syfo_oppfolgingsplan",
            )
            logger.info("Dialog opprettet i dialogporten")
        }
    }
}
