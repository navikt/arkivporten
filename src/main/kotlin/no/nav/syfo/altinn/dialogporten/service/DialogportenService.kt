package no.nav.syfo.altinn.dialogporten.service

import no.nav.syfo.API_V1_PATH
import no.nav.syfo.altinn.dialogporten.client.IDialogportenClient
import no.nav.syfo.altinn.dialogporten.domain.Attachment
import no.nav.syfo.altinn.dialogporten.domain.AttachmentUrlConsumerType
import no.nav.syfo.altinn.dialogporten.domain.Content
import no.nav.syfo.altinn.dialogporten.domain.ContentValueItem
import no.nav.syfo.altinn.dialogporten.domain.Dialog
import no.nav.syfo.altinn.dialogporten.domain.Url
import no.nav.syfo.altinn.dialogporten.domain.create
import no.nav.syfo.document.api.v1.DOCUMENT_API_PATH
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentEntity
import no.nav.syfo.document.db.DocumentStatus
import no.nav.syfo.util.logger

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

    private fun DocumentEntity.toDialog(): Dialog {
        return Dialog(
            serviceResource = "urn:altinn:resource:$dialogRessurs",
            party = "urn:altinn:organization:identifier-no:$orgnumber",
            externalReference = documentId.toString(),
            content = Content.create(
                title = dialogTitle,
                summary = dialogSummary,
            ),
            isApiOnly = true,
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
