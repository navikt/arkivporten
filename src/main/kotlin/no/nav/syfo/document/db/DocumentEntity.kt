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
    val orgNumber: String,
    val dialogTitle: String,
    val dialogSummary: String,
    val linkId: UUID,
    val status: DocumentStatus = DocumentStatus.RECEIVED,
    val isRead: Boolean = false,
    val dialogId: UUID?,
    val created: Instant? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentEntity

        if (id != other.id) return false
        if (isRead != other.isRead) return false
        if (documentId != other.documentId) return false
        if (type != other.type) return false
        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false
        if (orgNumber != other.orgNumber) return false
        if (dialogTitle != other.dialogTitle) return false
        if (dialogSummary != other.dialogSummary) return false
        if (linkId != other.linkId) return false
        if (status != other.status) return false
        if (dialogId != other.dialogId) return false
        if (created != other.created) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + isRead.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + orgNumber.hashCode()
        result = 31 * result + dialogTitle.hashCode()
        result = 31 * result + dialogSummary.hashCode()
        result = 31 * result + linkId.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (dialogId?.hashCode() ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        return result
    }
}
