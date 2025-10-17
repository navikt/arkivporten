package no.nav.syfo.application

import kotlin.String

data class ClientProperties(
    val altinnTilgangerBaseUrl: String,
    val eregBaseUrl: String,
    val electorPath: String,

    ) {
    companion object {
        fun createForLocal() = ClientProperties(
            altinnTilgangerBaseUrl = "https://altinn-tilganger-api.dev.intern.nav.no",
            eregBaseUrl = "",
            electorPath = "/elector",
        )

        fun createFromEnvVars() =
            ClientProperties(
                eregBaseUrl = getEnvVar("EREG_BASE_URL"),
                altinnTilgangerBaseUrl = getEnvVar("ALTINN_TILGANGER_BASE_URL"),
                electorPath = getEnvVar("ELECTOR_PATH"),

            )
    }
}
