package no.nav.syfo.dialogporten.domain

data class CreateDialogRequest(
    val orgnr: String,
    val title: String,
    val summary: String,
    val externalReference: String,
    val isApiOnly: Boolean = true,
    val transmissions: List<Transmission>,
)
