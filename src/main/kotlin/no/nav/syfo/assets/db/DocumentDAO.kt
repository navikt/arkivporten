package no.nav.syfo.assets.db

import java.util.UUID
import no.nav.syfo.assets.api.v1.AssetType

enum class DocumentStatus {
    RECEIVED,
    PENDING,
    COMPLETED,
    ERROR
}

data class DocumentDAO(
    val id: Long? = null,
    val documentId: UUID,
    val orgnumber: String,
    val type: AssetType,
    val content: ByteArray,
    val isRead: Boolean = false,
    val messageId: UUID?,
    val status: DocumentStatus = DocumentStatus.RECEIVED
)
