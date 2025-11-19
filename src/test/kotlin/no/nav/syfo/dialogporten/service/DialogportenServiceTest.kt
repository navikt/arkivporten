package no.nav.syfo.dialogporten.service

import document
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import no.nav.syfo.dialogporten.client.IDialogportenClient
import no.nav.syfo.dialogporten.domain.Dialog
import no.nav.syfo.document.api.v1.DocumentType
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.db.DocumentStatus
import java.util.UUID

class DialogportenServiceTest : DescribeSpec({
    val dialogportenClient = mockk<IDialogportenClient>()
    val documentDAO = mockk<DocumentDAO>()
    val publicIngressUrl = "https://test.nav.no"

    val dialogportenService = DialogportenService(
        dialogportenClient = dialogportenClient,
        documentDAO = documentDAO,
        publicIngressUrl = publicIngressUrl
    )

    beforeTest {
        clearAllMocks()
    }

    describe("sendDocumentsToDialogporten") {
        context("when there are no documents to send") {
            it("should not call dialogporten client") {
                // Arrange
                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns emptyList()

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                coVerify(exactly = 1) { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) }
                coVerify(exactly = 0) { dialogportenClient.createDialog(any()) }
                coVerify(exactly = 0) { documentDAO.update(any()) }
            }
        }

        context("when there is one document to send") {
            it("should send document to dialogporten and update status to COMPLETED") {
                // Arrange
                val documentEntity = document().toDocumentEntity()
                val dialogId = UUID.randomUUID()
                val dialogSlot = slot<Dialog>()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(documentEntity)
                coEvery { dialogportenClient.createDialog(capture(dialogSlot)) } returns dialogId
                coEvery { documentDAO.update(any()) } returns true

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                coVerify(exactly = 1) { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) }
                coVerify(exactly = 1) { dialogportenClient.createDialog(any()) }
                coVerify(exactly = 1) {
                    documentDAO.update(match {
                        it.dialogId == dialogId && it.status == DocumentStatus.COMPLETED
                    })
                }

                val capturedDialog = dialogSlot.captured
                capturedDialog.party shouldBe "urn:altinn:organization:identifier-no:${documentEntity.orgNumber}"
                capturedDialog.externalReference shouldBe documentEntity.documentId.toString()
                capturedDialog.content.title.value.first().value shouldBe documentEntity.dialogTitle
                capturedDialog.content.summary?.value?.first()?.value shouldBe documentEntity.dialogSummary
                capturedDialog.isApiOnly shouldBe true
                capturedDialog.attachments?.size shouldBe 1
                capturedDialog.attachments?.first()?.displayName?.first()?.value shouldBe "${documentEntity.type.displayName}.pdf"
            }
        }

        context("when there are multiple documents to send") {
            it("should send all documents to dialogporten") {
                // Arrange
                val doc1 = document().toDocumentEntity()
                val doc2 = document().toDocumentEntity().copy(type = DocumentType.OPPFOLGINGSPLAN)
                val dialogId1 = UUID.randomUUID()
                val dialogId2 = UUID.randomUUID()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(doc1, doc2)
                coEvery { dialogportenClient.createDialog(any()) } returnsMany listOf(dialogId1, dialogId2)
                coEvery { documentDAO.update(any()) } returns true

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                coVerify(exactly = 1) { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) }
                coVerify(exactly = 2) { dialogportenClient.createDialog(any()) }
                coVerify(exactly = 2) { documentDAO.update(any()) }
            }
        }

        context("when dialogporten client throws exception") {
            it("should log error and continue without updating document status") {
                // Arrange
                val documentEntity = document().toDocumentEntity()
                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(documentEntity)
                coEvery { dialogportenClient.createDialog(any()) } throws RuntimeException("Dialogporten error")

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                coVerify(exactly = 1) { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) }
                coVerify(exactly = 1) { dialogportenClient.createDialog(any()) }
                coVerify(exactly = 0) { documentDAO.update(any()) }
            }
        }

        context("when one document fails but others succeed") {
            it("should continue processing remaining documents") {
                // Arrange
                val doc1 = document().toDocumentEntity()
                val doc2 = document().toDocumentEntity()
                val doc3 = document().toDocumentEntity()
                val dialogId2 = UUID.randomUUID()
                val dialogId3 = UUID.randomUUID()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(doc1, doc2, doc3)
                coEvery { documentDAO.update(any()) } returns true

                // First call succeeds, second fails, third succeeds
                var callCount = 0
                coEvery { dialogportenClient.createDialog(any()) } answers {
                    callCount++
                    when (callCount) {
                        1 -> dialogId2
                        2 -> throw RuntimeException("Error")
                        3 -> dialogId3
                        else -> throw RuntimeException("Unexpected call")
                    }
                }

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                coVerify(exactly = 1) { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) }
                coVerify(exactly = 3) { dialogportenClient.createDialog(any()) }
                coVerify(exactly = 2) { documentDAO.update(any()) } // Only 2 successful updates
            }
        }

        context("when document has JSON content type") {
            it("should create dialog with correct display name") {
                // Arrange
                val documentEntity = document().toDocumentEntity().copy(contentType = "application/json")
                val dialogId = UUID.randomUUID()
                val dialogSlot = slot<Dialog>()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(documentEntity)
                coEvery { dialogportenClient.createDialog(capture(dialogSlot)) } returns dialogId
                coEvery { documentDAO.update(any()) } returns true

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                val capturedDialog = dialogSlot.captured
                capturedDialog.attachments?.first()?.displayName?.first()?.value shouldBe "${documentEntity.type.displayName}.json"
            }
        }

        context("when document content includes correct resource URN") {
            it("should use nav_syfo_dialog resource") {
                // Arrange
                val documentEntity = document().toDocumentEntity()
                val dialogId = UUID.randomUUID()
                val dialogSlot = slot<Dialog>()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(documentEntity)
                coEvery { dialogportenClient.createDialog(capture(dialogSlot)) } returns dialogId
                coEvery { documentDAO.update(any()) } returns true

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                val capturedDialog = dialogSlot.captured
                capturedDialog.serviceResource shouldBe "urn:altinn:resource:nav_syfo_dialog"
            }
        }

        context("when document has attachment URL") {
            it("should create correct document link with linkId") {
                // Arrange
                val documentEntity = document().toDocumentEntity()
                val dialogId = UUID.randomUUID()
                val dialogSlot = slot<Dialog>()

                coEvery { documentDAO.getDocumentsByStatus(DocumentStatus.RECEIVED) } returns listOf(documentEntity)
                coEvery { dialogportenClient.createDialog(capture(dialogSlot)) } returns dialogId
                coEvery { documentDAO.update(any()) } returns true

                // Act
                dialogportenService.sendDocumentsToDialogporten()

                // Assert
                val capturedDialog = dialogSlot.captured
                val attachmentUrl = capturedDialog.attachments?.first()?.urls?.first()?.url
                attachmentUrl shouldBe "$publicIngressUrl/api/v1/documents/${documentEntity.linkId}"
            }
        }
    }
})
