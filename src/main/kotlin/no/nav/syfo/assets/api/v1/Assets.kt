package no.nav.syfo.assets.api.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import java.util.UUID
import no.nav.syfo.assets.db.DocumentDAO

data class Asset(
    val assetId: UUID,
    val orgnumber: String,
    val type: AssetType,
    val content: ByteArray,
) {
    fun toDocumentDAO(): DocumentDAO {
        return DocumentDAO(
            documentId = assetId,
            orgnumber = orgnumber,
            type = type,
            content = content,
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
