package no.nav.syfo.document.db

import java.time.LocalDateTime
import java.util.UUID
import no.nav.syfo.document.api.v1.AssetType

enum class DocumentStatus {
    RECEIVED,
    PENDING,
    COMPLETED,
    ERROR
}

data class DocumentDAO(
    val id: Long? = null,
    val documentId: UUID,
    val type: AssetType,
    val content: ByteArray,
    val contentType: String,
    val orgnumber: String,
    val messageTitle: String,
    val messageSummary: String,
    val linkId: UUID,
    val status: DocumentStatus = DocumentStatus.RECEIVED,
    val isRead: Boolean = false,
    val messageId: UUID?,
    val created: LocalDateTime? = null,
)
