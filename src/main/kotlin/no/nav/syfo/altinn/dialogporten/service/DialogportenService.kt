package no.nav.syfo.altinn.dialogporten.service

import com.fasterxml.uuid.Generators
import no.nav.syfo.API_V1_PATH
import no.nav.syfo.altinn.dialogporten.client.IDialogportenClient
import no.nav.syfo.altinn.dialogporten.domain.Attachment
import no.nav.syfo.altinn.dialogporten.domain.AttachmentUrlConsumerType
import no.nav.syfo.altinn.dialogporten.domain.Content
import no.nav.syfo.altinn.dialogporten.domain.ContentValueItem
import no.nav.syfo.altinn.dialogporten.domain.Dialog
import no.nav.syfo.altinn.dialogporten.domain.Transmission
import no.nav.syfo.altinn.dialogporten.domain.Url
import no.nav.syfo.altinn.dialogporten.domain.create
import no.nav.syfo.document.api.v1.DOCUMENT_API_PATH
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.document.db.DocumentStatus
import no.nav.syfo.util.logger
import java.time.Instant
import java.util.UUID

class DialogportenService(
    private val dialogportenClient: IDialogportenClient,
    private val documentDAO: DocumentDAO,
    private val publicIngressUrl: String
) {
    private val logger = logger()
    private val dialogRessurs = "nav_syfo_dialog"

    suspend fun sendDocumentsToDialogporten() {
        val documentsToSend = getDocumentsToSend()
        logger.info("Found ${documentsToSend.size} documents to send to dialogporten")

        for (document in documentsToSend) {
            val fullDocumentLink = createDocumentLink(document.linkId.toString())
            val transmissionId = Generators.timeBasedEpochGenerator().generate()

            try {
                if (document.dialog.dialogportenId != null) {
                    // add transmission to existing dialog
                    val transmission = document.toTransmission(transmissionId)
                    dialogportenClient.addTransmission(transmission, document.dialog.dialogportenId)
                    documentDAO.update(
                        document.copy(
                            transmissionId = transmissionId,
                            status = DocumentStatus.COMPLETED,
                            updated = Instant.now()
                        )
                    )
                } else {
                    // create new dialog with transmission
                    val dialog = document.toDialogWithTransmission(transmissionId)
                    val dialogId = dialogportenClient.createDialog(dialog)
                    documentDAO.update(
                        document.copy(
                            transmissionId = transmissionId,
                            status = DocumentStatus.COMPLETED,
                            dialog = document.dialog.copy(
                                dialogportenId = dialogId,
                                updated = Instant.now()
                            ),
                            updated = Instant.now()
                        )
                    )
                }
                logger.info("Sent document ${document.id} to dialogporten, with link $fullDocumentLink and content type ${document.contentType}")
            } catch (ex: Exception) {
                logger.error("Failed to send document ${document.id} to dialogporten", ex)
            }
        }
    }

    private fun getDocumentsToSend() = documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED)

    private fun createDocumentLink(linkId: String): String =
        "$publicIngressUrl$API_V1_PATH$DOCUMENT_API_PATH/$linkId"

    private fun getDocumentDisplayName(document: DocumentEntity): String {
        val fileType = when (document.contentType) {
            "application/pdf" -> "pdf"
            "application/json" -> "json"
            else -> throw IllegalArgumentException("Unsupported document content type ${document.contentType}")
        }
        return "${document.type.displayName}.$fileType"
    }

    private fun DocumentEntity.toDialogWithTransmission(transmissionId: UUID): Dialog {
        return Dialog(
            serviceResource = "urn:altinn:resource:$dialogRessurs",
            party = "urn:altinn:organization:identifier-no:${dialog.orgNumber}",
            externalReference = "syfo-arkivporten",
            content = Content.create(
                title = dialog.title,
                summary = dialog.summary,
            ),
            isApiOnly = true,
            transmissions = listOf(
                toTransmission(transmissionId)
            )
        )
    }

    private fun DocumentEntity.toTransmission(transmissionId: UUID): Transmission {
        return Transmission(
            id = transmissionId,
            content = Content.create(
                title = title,
                summary = summary,
            ),
            type = Transmission.TransmissionType.Information,
            sender = Transmission.Sender("ServiceOwner"),
            externalReference = documentId.toString(),
            extendedType = type.name,
            attachments = listOf(
                Attachment(
                    displayName = listOf(
                        ContentValueItem(
                            getDocumentDisplayName(this),
                            "nb"
                        ),
                    ),
                    urls = listOf(
                        Url(
                            url = createDocumentLink(linkId.toString()),
                            mediaType = contentType,
                            consumerType = AttachmentUrlConsumerType.Api,
                        ),
                    ),
                ),
            ),
        )
    }
}
