package no.nav.syfo.altinn.dialogporten.client

import no.nav.syfo.altinn.dialogporten.domain.Dialog
import no.nav.syfo.altinn.dialogporten.domain.Transmission
import java.util.UUID

class FakeDialogportenClient() : IDialogportenClient {
    override suspend fun createDialog(dialog: Dialog): UUID {
        return UUID.randomUUID()
    }

    override suspend fun addTransmission(
        transmission: Transmission,
        dialogId: UUID
    ): UUID {
        return UUID.randomUUID()
    }
}
