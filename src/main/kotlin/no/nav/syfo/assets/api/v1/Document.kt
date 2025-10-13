package no.nav.syfo.assets.api.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import java.util.UUID
import no.nav.syfo.assets.db.DocumentDAO

data class Document(
    val documentId: UUID,
    val type: AssetType,
    val content: ByteArray,
    val contentType: String,
    val orgnumber: String,
    val messageTitle: String,
    val messageSummary: String,
) {
    fun toDocumentDAO(): DocumentDAO {
        return DocumentDAO(
            documentId = documentId,
            type = type,
            content = content,
            contentType = contentType,
            orgnumber = orgnumber,
            messageTitle = messageTitle,
            messageSummary = messageSummary,
            linkId = UUID.randomUUID(),
            messageId = null,
        )
    }
}

enum class AssetType {
    DIALOGMOTE,
    OPPFOLGINGSPLAN,

    @JsonEnumDefaultValue
    UNDEFINED,
}
