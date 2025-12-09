package no.nav.syfo.application

import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.texas.TexasEnvironment

interface Environment {
    val database: DatabaseEnvironment
    val texas: TexasEnvironment
    val clientProperties: ClientProperties
    val publicIngressUrl: String
    val allowedClientIds: List<String>
}

const val NAIS_DATABASE_ENV_PREFIX = "ARKIVPORTEN_DB"

data class NaisEnvironment(
    override val database: DatabaseEnvironment = DatabaseEnvironment.createFromEnvVars(),
    override val texas: TexasEnvironment = TexasEnvironment.createFromEnvVars(),
    override val clientProperties: ClientProperties = ClientProperties.createFromEnvVars(),
    override val publicIngressUrl: String = getEnvVar("PUBLIC_INGRESS_URL"),
    override val allowedClientIds: List<String> = getAllowedClientIds(getEnvVar("NAIS_CLUSTER_NAME")),
    ) : Environment

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun isLocalEnv(): Boolean =
    getEnvVar("NAIS_CLUSTER_NAME", "local") == "local"

fun isProdEnv(): Boolean =
    getEnvVar("NAIS_CLUSTER_NAME", "local") == "prod-gcp"

private fun getAllowedClientIds(clusterName: String): List<String> = listOf(
    "${clusterName}:team-esyfo:syfo-oppfolgingsplan-backend",
    "${clusterName}:teamsykefravr:isdialogmote",
)

data class LocalEnvironment(
    override val database: DatabaseEnvironment = DatabaseEnvironment.createForLocal(),
    override val texas: TexasEnvironment = TexasEnvironment.createForLocal(),
    override val clientProperties: ClientProperties = ClientProperties.createForLocal(),
    override val publicIngressUrl: String = "http://localhost:8080",
    override val allowedClientIds: List<String> = getAllowedClientIds("local"),
) : Environment
