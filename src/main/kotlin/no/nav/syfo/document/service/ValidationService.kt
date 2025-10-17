package no.nav.syfo.document.service

import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.OrganisasjonPrincipal
import no.nav.syfo.application.auth.Principal
import no.nav.syfo.application.auth.maskinportenIdToOrgnumber
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.ereg.EregService
import no.nav.syfo.util.logger

class ValidationService(
    val altinnTilgangerService: AltinnTilgangerService,
    val eregService: EregService,
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

    suspend fun validateMaskinportenTilgang(principal: OrganisasjonPrincipal, documentDAO: DocumentEntity) {
        val orgnumberFromToken = maskinportenIdToOrgnumber(principal.ident)
        if (orgnumberFromToken != documentDAO.orgnumber) {
            val organisasjon = eregService.getOrganization(documentDAO.orgnumber)
            if (organisasjon.inngaarIJuridiskEnheter?.filter { it.organisasjonsnummer == orgnumberFromToken }
                    .isNullOrEmpty()) {
                logger.warn(
                    "Maskinporten orgnummer ${orgnumberFromToken} does not match document orgnummer ${documentDAO.orgnumber} or any parent organization."
                )
                throw ApiErrorException.ForbiddenException("Access denied. Invalid organization.")
            }
        }
    }
}
