package no.nav.syfo.altinntilganger

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.altinntilganger.client.IAltinnTilgangerClient
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.application.exception.UpstreamRequestException
import no.nav.syfo.document.api.v1.DocumentType
import no.nav.syfo.util.logger

class AltinnTilgangerService(
    val altinnTilgangerClient: IAltinnTilgangerClient,
) {
    suspend fun validateTilgangToOrganisasjon(
        brukerPrincipal: BrukerPrincipal,
        orgnummer: String,
        documentType: DocumentType
    ) {
        try {
            logger.info("Henter altinn-tilganger for bruker")
            val tilganger = altinnTilgangerClient.hentTilganger(brukerPrincipal)

            logger.info(ObjectMapper().writeValueAsString(tilganger))
            val requiredResource = requiredResourceByDocumentType[documentType]
                ?: throw ApiErrorException.InternalServerErrorException("Ukjent dokumenttype $documentType")
            if (tilganger?.orgNrTilTilganger[orgnummer]?.contains(requiredResource) != true)
                throw ApiErrorException.ForbiddenException("Bruker har ikke tilgang til organisasjon $orgnummer")
        } catch (e: UpstreamRequestException) {
            logger.error("Feil ved henting av tilgang til organisasjon $orgnummer", e)
            throw ApiErrorException.InternalServerErrorException("Feil ved henting av altinn-tilganger")
        }
    }

    companion object {
        private val logger = logger()
        val requiredResourceByDocumentType = mapOf(
            DocumentType.OPPFOLGINGSPLAN to "nav_syfo_oppfolgingsplan",
            DocumentType.DIALOGMOTE to "nav_syfo_dialogmote",
        )
    }
}
