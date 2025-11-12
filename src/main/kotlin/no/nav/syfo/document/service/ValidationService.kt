package no.nav.syfo.document.service

import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.altinntilganger.AltinnTilgangerService.Companion.requiredResourceByDocumentType
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.SystemPrincipal
import no.nav.syfo.application.auth.Principal
import no.nav.syfo.application.auth.maskinportenIdToOrgnumber
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.document.api.v1.DocumentType
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.ereg.EregService
import no.nav.syfo.altinn.pdp.client.System
import no.nav.syfo.altinn.pdp.service.PdpService
import no.nav.syfo.util.logger

class ValidationService(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val eregService: EregService,
    private val pdpService: PdpService
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
            is SystemPrincipal -> validateMaskinportenTilgang(principal, documentDAO)
        }
    }


    suspend private fun validateAltTilgang(principal: BrukerPrincipal, documentDAO: DocumentEntity) {
        altinnTilgangerService.validateTilgangToOrganisasjon(
            principal,
            documentDAO.orgnumber,
            documentDAO.type
        )
    }

    suspend fun validateMaskinportenTilgang(principal: SystemPrincipal, documentDAO: DocumentEntity) {
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
        validateAltinnRessursTilgang(principal, documentDAO.type)
    }

    private suspend fun validateAltinnRessursTilgang(principal: SystemPrincipal, documentType: DocumentType) {
        val requiredRessurs = requiredResourceByDocumentType[documentType]
            ?: throw ApiErrorException.InternalServerErrorException("Could not find resource for document type $documentType")

        val hasAccess = pdpService.hasAccessToResource(
            System(principal.systemUserId),
            setOf(
                maskinportenIdToOrgnumber(principal.ident),
                maskinportenIdToOrgnumber(principal.systemOwner)
            ),
            requiredRessurs
        )
        if (!hasAccess) {
            throw ApiErrorException.ForbiddenException("Access denied to resource $requiredRessurs, for system user ${principal.systemUserId}", )
        }

    }
}
