package no.nav.syfo.document.api.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import java.util.UUID
import no.nav.syfo.document.db.DocumentEntity

data class Document(
    val documentId: UUID,
    val type: DocumentType,
    val content: ByteArray,
    val contentType: String,
    val orgnumber: String,
    val dialogTitle: String,
    val dialogSummary: String,
) {
    fun toDocumentEntity(): DocumentEntity {
        return DocumentEntity(
            documentId = documentId,
            type = type,
            content = content,
            contentType = contentType,
            orgnumber = orgnumber,
            dialogTitle = dialogTitle,
            dialogSummary = dialogSummary,
            linkId = UUID.randomUUID(),
            dialogId = null,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (documentId != other.documentId) return false
        if (type != other.type) return false
        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false
        if (orgnumber != other.orgnumber) return false
        if (dialogTitle != other.dialogTitle) return false
        if (dialogSummary != other.dialogSummary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + orgnumber.hashCode()
        result = 31 * result + dialogTitle.hashCode()
        result = 31 * result + dialogSummary.hashCode()
        return result
    }
}

enum class DocumentType {
    DIALOGMOTE,
    OPPFOLGINGSPLAN,

    @JsonEnumDefaultValue
    UNDEFINED,
}
