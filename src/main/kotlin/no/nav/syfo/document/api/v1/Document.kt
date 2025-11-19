package no.nav.syfo.document.api.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import no.nav.syfo.document.db.DialogEntity
import java.util.UUID
import no.nav.syfo.document.db.DocumentEntity

data class Document(
    val documentId: UUID,
    val type: DocumentType,
    val content: ByteArray,
    val contentType: String,
    val fnr: String,
    val fullName: String?,
    val orgNumber: String,
    val title: String,
    val summary: String?,
) {
    fun toDocumentEntity(dialog: DialogEntity): DocumentEntity {
        return DocumentEntity(
            documentId = documentId,
            type = type,
            content = content,
            contentType = contentType,
            title = title,
            summary = summary,
            linkId = UUID.randomUUID(),
            dialog = dialog,
        )
    }

    fun toDialogEntity(): DialogEntity {
        val nameOrFnr = fullName ?: fnr
        return DialogEntity(
            title = "Sykefraværsoppfølging for $nameOrFnr (f. ${fnrToBirthDate(fnr)})",
            summary = """
                Her finner du alle dialogmøtebrev fra Nav og oppfølgingsplaner utarbeidet av nærmeste leder for $nameOrFnr.
                Innholdet er tilgjengelig i 4 måneder fra delingsdatoen. 
            """.trimIndent(),
            fnr = fnr,
            orgNumber = orgNumber,
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
        if (orgNumber != other.orgNumber) return false
        if (title != other.title) return false
        if (summary != other.summary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + orgNumber.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + summary.hashCode()
        return result
    }
}

enum class DocumentType(val displayName: String) {
    DIALOGMOTE("Dialogmøte"),
    OPPFOLGINGSPLAN("Oppfølgingsplan"),

    @JsonEnumDefaultValue
    UNDEFINED("Dokument"),
}
