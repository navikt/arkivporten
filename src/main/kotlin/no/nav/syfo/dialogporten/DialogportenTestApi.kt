package no.nav.syfo.dialogporten

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.dialogporten.client.DialogportenClient
import no.nav.syfo.dialogporten.domain.CreateDialogRequest

// Kun for test av opprettelse av dialog i dialogporten. Kan slettes senere.
fun Route.registerDialogportenTestApi(
    dialogportenClient: DialogportenClient,
) {
    route("/dialogporten") {
        post("/test") {
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = "15649202", // KONTANT PRAGMATISK FJELLREV
                    title = "Test dialog",
                    summary = "Denne dialogen er opprettet for å teste dialogporten",
                    externalReference = "test-${System.currentTimeMillis()}",
                    isApiOnly = false,
                    transmissions = emptyList(),
                ),
                ressurs = "nav_syfo_oppfolgingsplan",
            )
        }
    }
}
