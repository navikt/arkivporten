package no.nav.syfo.assets.api.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

data class Asset(
    val assetId: String,
    val orgnumber: String,
    val type: AssetType,
    val pdf: ByteArray,
)

enum class AssetType {
    DIALOGMOTE,
    OPPFOLGINGSPLAN,
    @JsonEnumDefaultValue
    UNDEFINED,
}
