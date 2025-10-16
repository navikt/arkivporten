package no.nav.syfo.ereg

import no.nav.syfo.ereg.client.IEaregClient
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.application.exception.UpstreamRequestException
import no.nav.syfo.ereg.client.Organisasjon

class EregService(private val eregClient: IEaregClient) {
    suspend fun getOrganization(
        orgnumber: String
    ): Organisasjon {
        return try {
            eregClient.getOrganisasjon(orgnummer = orgnumber)
        } catch (e: UpstreamRequestException) {
            throw ApiErrorException.InternalServerErrorException(
                "Could not get organization",
                e
            )
        }
            ?: throw ApiErrorException.BadRequestException("Unable to look up the organization")
    }
}
