package no.nav.syfo.document.db

import java.util.UUID
import java.time.Instant
import no.nav.syfo.document.api.v1.DocumentType

enum class DocumentStatus {
    RECEIVED,
    PENDING,
    COMPLETED,
    ERROR
}

data class DocumentEntity(
    val id: Long? = null,
    val documentId: UUID,
    val type: DocumentType,
    val content: ByteArray,
    val contentType: String,
    val orgnumber: String,
    val messageTitle: String,
    val messageSummary: String,
    val linkId: UUID,
    val status: DocumentStatus = DocumentStatus.RECEIVED,
    val isRead: Boolean = false,
    val messageId: UUID?,
    val created: Instant? = null,
)
