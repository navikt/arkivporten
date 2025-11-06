package no.nav.syfo.pdp.service

import no.nav.syfo.pdp.client.Bruker
import no.nav.syfo.pdp.client.IPdpClient
import no.nav.syfo.pdp.client.harTilgang

class PdpService(
    private val pdpClient: IPdpClient,
) {

    suspend fun hasAccessToResource(
        bruker: Bruker,
        orgnrSet: Set<String>,
        ressurs: String
    ): Boolean {
        val pdpResponse = pdpClient.authorize(bruker, orgnrSet, ressurs)
        return pdpResponse.harTilgang()
    }
}
