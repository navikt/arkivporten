package no.nav.syfo.dialogporten.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.syfo.API_V1_PATH
import no.nav.syfo.application.leaderelection.LeaderElection
import no.nav.syfo.dialogporten.client.IDialogportenClient
import no.nav.syfo.dialogporten.domain.Content
import no.nav.syfo.dialogporten.domain.ContentValueItem
import no.nav.syfo.dialogporten.domain.Dialog
import no.nav.syfo.dialogporten.domain.Transmission
import no.nav.syfo.dialogporten.domain.create
import no.nav.syfo.document.api.v1.DOCUMENT_API_PATH
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.document.db.DocumentStatus
import no.nav.syfo.util.logger

class SendDialogTask(
    private val leaderElection: LeaderElection,
    private val dialogportenClient: IDialogportenClient,
    private val documentDAO: DocumentDAO,
    private val publicIngressUrl: String
) {
    private val logger = logger()
    private val dialogRessurs = "nav_syfo_dialog"

    suspend fun runTask() = coroutineScope {
        try {
            while (isActive) {
                if (leaderElection.isLeader()) {
                    try {
                        logger.info("Starting task for sending documents to dialogporten")
                        val documentsToSend = getDocumentsToSend()
                        logger.info("Found ${documentsToSend.size} documents to send to dialogporten")
                        sendDocumentsToDialogporten(documentsToSend)
                    } catch (ex: Exception) {
                        logger.error("Could not send dialogs to dialogporten", ex)
                    }
                }
                // delay for  5 minutes before checking again
                delay(5 * 60 * 1000)
            }
        } catch (ex: CancellationException) {
            logger.info("Cancelled SendDialogTask", ex)
        }
    }

    private fun getDocumentsToSend() = documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED)

    private suspend fun sendDocumentsToDialogporten(documentsToSend: List<DocumentEntity>) {
        for (document in documentsToSend) {
            val fullDocumentLink = createDocumentLink(document.linkId.toString())
            try {
                val dialog = document.toDialog()
                val dialogId = dialogportenClient.createDialog(dialog)
                documentDAO.update(
                    document.copy(
                        dialogId = dialogId,
                        status = DocumentStatus.COMPLETED
                    )
                )
                logger.info("Sent document ${document.id} to dialogporten, with link $fullDocumentLink and content type ${document.contentType}")
            } catch (ex: Exception) {
                logger.error("Failed to send document ${document.id} to dialogporten", ex)
            }
        }
    }

    private fun createDocumentLink(linkId: String): String =
        "$publicIngressUrl$API_V1_PATH$DOCUMENT_API_PATH/$linkId"

    private fun DocumentEntity.toDialog(): Dialog {
        return Dialog(
            serviceResource = "urn:altinn:resource:$dialogRessurs",
            party = "urn:altinn:organization:identifier-no:$orgnumber",
            externalReference = documentId.toString(),
            content = Content.create(
                title = dialogTitle,
                summary = dialogSummary,
            ),
            isApiOnly = false,
            transmissions = listOf(
                Transmission(
                    type = Transmission.TransmissionType.Information,
                    extendedType = type.name,
                    sender = Transmission.Sender("ServiceOwner"),
                    // TODO: Update content with meaningful title and summary
                    content = Content.create("transmissionTitle", "transmissionSummary"),
                    attachments = listOf(
                        Transmission.Attachment(
                            // TODO: Update displayName
                            displayName = listOf(
                                ContentValueItem(
                                    "Oppfølgingsplan.pdf",
                                    "nb"
                                ),
                            ),
                            urls = listOf(
                                Transmission.Url(
                                    url = createDocumentLink(linkId.toString()),
                                    mediaType = contentType,
                                    consumerType = Transmission.AttachmentUrlConsumerType.Api,
                                ),
                            ),
                        ),
                    )
                )
            ),
        )
    }
}
