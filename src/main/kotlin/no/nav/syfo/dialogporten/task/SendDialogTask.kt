package no.nav.syfo.dialogporten.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.syfo.application.leaderelection.LeaderElection
import no.nav.syfo.dialogporten.client.DialogportenClient
import no.nav.syfo.dialogporten.domain.Content
import no.nav.syfo.dialogporten.domain.ContentValueItem
import no.nav.syfo.dialogporten.domain.CreateDialogRequest
import no.nav.syfo.dialogporten.domain.Transmission
import no.nav.syfo.dialogporten.domain.create
import no.nav.syfo.document.api.v1.DocumentType
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.document.db.DocumentStatus
import no.nav.syfo.util.logger

class SendDialogTask(
    private val leaderElection: LeaderElection,
    private val dialogportenClient: DialogportenClient,
    private val documentDAO: DocumentDAO
) {
    private val logger = logger()
    private val linkBaseUrl = "https://arkivporten.ekstern.dev.nav.no/api/v1/documents/"

    suspend fun runTask() = coroutineScope {
        try {
            while (isActive) {
                if (leaderElection.isLeader()) {
                    try {
                        val documentsToSend = getDocumentsToSend()
                        sendDocumentsToDialogporten(documentsToSend)
                    } catch (ex: Exception) {
                        logger.error("Could not send dialogs to dialogporten", ex)
                    }
                }
                // Sleep for a while before checking again
                delay(5 * 60 * 1000) // 5 minutes
            }
        } catch (ex: CancellationException) {
            logger.info("cancelled delete data job", ex)
        }
    }

    private fun getDocumentsToSend() = documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED)

    private suspend fun sendDocumentsToDialogporten(documentsToSend: List<DocumentEntity>) {
        for (document in documentsToSend) {
            val fullDocumentLink = "$linkBaseUrl${document.linkId}"
            try {
                val dialogId = dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = document.orgnumber,
                        title = document.dialogTitle,
                        summary = document.dialogSummary,
                        externalReference = document.documentId.toString(),
                        isApiOnly = false,
                        transmissions = listOf(
                            Transmission(
                                type = Transmission.TransmissionType.Information,
                                sender = Transmission.Sender("ServiceOwner"),
                                content = Content.create("transmissionTitle", "transmissionSummary"),
                                attachments = listOf(
                                    Transmission.Attachment(
                                        displayName = listOf(
                                            ContentValueItem(
                                                "attachmentDisplayName",
                                                "nb"
                                            ),
                                        ),
                                        urls = listOf(
                                            Transmission.Url(
                                                url = fullDocumentLink,
                                                mediaType = document.contentType,
                                                consumerType = Transmission.AttachmentUrlConsumerType.Gui,
                                            ),
                                            Transmission.Url(
                                                url = fullDocumentLink,
                                                mediaType = document.contentType,
                                                consumerType = Transmission.AttachmentUrlConsumerType.Api,
                                            ),
                                        ),
                                    ),
                                )
                            )
                        ),
                    ),
                    ressurs = documentTypeToRessurs(document.type)
                )
                documentDAO.update(
                    document.copy(
                        dialogId = dialogId,
                        status = DocumentStatus.COMPLETED
                    )
                )
                logger.info("Sent document ${document.id} to dialogporten")
            } catch (ex: Exception) {
                logger.error("Failed to send document ${document.id} to dialogporten", ex)
            }
        }
    }

    private fun documentTypeToRessurs(type: DocumentType): String =
        when (type) {
            DocumentType.OPPFOLGINGSPLAN -> "nav_syfo_oppfolgingsplan"
            DocumentType.DIALOGMOTE -> "nav_syfo_dialogmote"
            DocumentType.UNDEFINED -> throw RuntimeException("Undefined document type $type")
        }
}
