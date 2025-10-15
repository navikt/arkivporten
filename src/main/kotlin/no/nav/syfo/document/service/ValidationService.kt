package no.nav.syfo.document.service

import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.OrganisasjonPrincipal
import no.nav.syfo.application.auth.Principal
import no.nav.syfo.application.auth.maskinportenIdToOrgnumber
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.util.logger

class ValidationService(
    val altinnTilgangerService: AltinnTilgangerService
) {
    companion object {
        val logger = logger()
    }

    suspend fun validateDocumentAccess(
        principal: Principal,
        documentDAO: DocumentEntity
    ) {
        when (principal) {
            is BrukerPrincipal -> validateAltTilgang(principal, documentDAO)
            is OrganisasjonPrincipal -> validateMaskinportenTilgang(principal, documentDAO)
        }
    }


    suspend private fun validateAltTilgang(principal: BrukerPrincipal, documentDAO: DocumentEntity) {
        altinnTilgangerService.validateTilgangToOrganisasjon(
            principal,
            documentDAO.orgnumber,
            documentDAO.type
        )
    }

    fun validateMaskinportenTilgang(principal: OrganisasjonPrincipal, documentDAO: DocumentEntity) {
        val orgnumberFromToken = maskinportenIdToOrgnumber(principal.ident)
        if (orgnumberFromToken != documentDAO.orgnumber) {
            logger.warn(
                "Maskinporten orgnummer ${orgnumberFromToken} does not match document orgnummer ${documentDAO.orgnumber}"
            )
            throw ApiErrorException.ForbiddenException("Access denied. Invalid organization.")
        }
    }
}
