package no.nav.syfo.pdp.client
import kotlinx.serialization.Serializable

@Serializable
data class PdpResponse(
    val response: List<DecisionResult>,
)

@Serializable
data class DecisionResult(
    val decision: Decision,
)

enum class Decision {
    Permit,
    Indeterminate,
    NotApplicable,
    Deny,
}

fun PdpResponse.resultat() = response.first().decision

fun PdpResponse.harTilgang(): Boolean = resultat() == Decision.Permit
