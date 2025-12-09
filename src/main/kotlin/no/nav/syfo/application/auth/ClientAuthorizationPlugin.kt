package no.nav.syfo.application.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.principal
import io.ktor.server.request.uri
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.application.isProdEnv
import no.nav.syfo.util.logger

private val logger = logger("no.nav.syfo.application.auth.ClientAuthorizationPlugin")

class ClientAuthorizationPluginConfig {
    lateinit var allowedClientIds: List<String>
}

val ClientAuthorizationPlugin = createRouteScopedPlugin(
    name = "ClientAuthorizationPlugin",
    createConfiguration = ::ClientAuthorizationPluginConfig,
) {
    val clientIds = pluginConfig.allowedClientIds

    val allowedClients = if (isProdEnv()) {
        clientIds
    } else {
        clientIds
            .plus("dev-gcp:nais:azure-token-generator")
    }

    onCall { call ->
        call.requireClient(allowedClients)
    }
}

private fun ApplicationCall.requireClient(allowedClients: List<String>) {
    val principal = principal<AzureAdPrincipal>()
        ?: throw ApiErrorException.UnauthorizedException("No user principal found in request")
    val callerClientId = principal.clientId
    if (!allowedClients.contains(callerClientId)) {
        logger.error(
            "Client authorization failed - expected: $allowedClients, actual: $callerClientId, path: ${request.uri}"
        )
        throw ApiErrorException.ForbiddenException("Caller is not authorized for this endpoint")
    }
}
