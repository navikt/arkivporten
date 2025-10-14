package no.nav.syfo.document.db

import document
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID
import no.nav.syfo.TestDB

class DocumentDbTest : DescribeSpec({
    val testDb = TestDB.database
    val documentDb = DocumentDb(testDb)
    beforeTest {
        TestDB.clearAllData()
    }

    describe("DocumentDb -> insert") {
        it("should return a generated id") {
            // Arrange
            val documentDAO = document().toDocumentDAO()
            // Act
            val id = documentDb.insert(documentDAO)
            // Assert
            id shouldNotBe null
            id shouldBeGreaterThan 0L
        }

        it("should persist the document with the correct fields") {
            // Arrange
            val documentDAO = document().toDocumentDAO()
            // Act
            val id = documentDb.insert(documentDAO)
            // Assert
            val retrievedDocument = documentDb.getById(id)
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentDAO, id)
        }
    }
    describe("DocumentDb -> update") {
        it("should return a generated id") {
            // Arrange
            val documentDAO = document().toDocumentDAO()
            // Act
            val id = documentDb.insert(documentDAO)
            val updatedDocumentDAO = documentDAO.copy(
                id = id,
                messageId = UUID.randomUUID(),
                status = DocumentStatus.COMPLETED,
                isRead = true,
            )
            documentDb.update(updatedDocumentDAO)
            val retrievedDocument = documentDb.getById(id)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(updatedDocumentDAO, id)
        }
    }

    describe("DocumentDb -> getById") {
        it("should return a DocumentDAO for the id") {
            // Arrange
            val documentDAO = document().toDocumentDAO()
            // Act
            val id = documentDb.insert(documentDAO)
            val retrievedDocument = documentDb.getById(id)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentDAO, id)
        }
    }

    describe("DocumentDb -> getByLinkId") {
        it("should return a DocumentDAO for the linktId") {
            // Arrange
            val documentDAO = document().toDocumentDAO()
            // Act
            val id = documentDb.insert(documentDAO)
            val retrievedDocument = documentDb.getByLinkId(documentDAO.linkId)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentDAO, id)
        }
    }
})

fun DocumentDAO.assertExpected(expected: DocumentDAO, id: Long) {
    this.id shouldBe id
    this.documentId shouldBe expected.documentId
    this.type shouldBe expected.type
    this.content shouldBe expected.content
    this.contentType shouldBe expected.contentType
    this.orgnumber shouldBe expected.orgnumber
    this.messageTitle shouldBe expected.messageTitle
    this.messageSummary shouldBe expected.messageSummary
    this.linkId shouldBe expected.linkId
    this.status shouldBe expected.status
    this.isRead shouldBe expected.isRead
    this.messageId shouldBe expected.messageId
}
