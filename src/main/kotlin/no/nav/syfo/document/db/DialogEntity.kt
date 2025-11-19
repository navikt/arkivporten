package no.nav.syfo.document.db

import java.time.Instant
import java.util.UUID

data class DialogEntity(
    val id: Long? = null,
    val title: String,
    val summary: String?,
    val fnr: String,
    val orgNumber: String,
    val dialogportenId: UUID? = null,
    val created: Instant? = null,
    val updated: Instant? = null,
)
