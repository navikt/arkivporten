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
}

enum class DocumentType {
    DIALOGMOTE,
    OPPFOLGINGSPLAN,

    @JsonEnumDefaultValue
    UNDEFINED,
}
