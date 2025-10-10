package no.nav.syfo.application

data class ClientProperties(
    val altinnTilgangerBaseUrl: String,
    val eregBaseUrl: String,
    val eregScope: String,
) {
    companion object {
        fun createForLocal() = ClientProperties(
            altinnTilgangerBaseUrl = "https://altinn-tilganger-api.dev.intern.nav.no",
            eregBaseUrl = "",
            eregScope = "ereg",
        )

        fun createFromEnvVars() =
            ClientProperties(
                eregBaseUrl = getEnvVar("EREG_BASE_URL"),
                altinnTilgangerBaseUrl = getEnvVar("ALTINN_TILGANGER_BASE_URL"),
                eregScope = getEnvVar("EREG_SCOPE"),
            )
    }
}
