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
            val documentEntity = document().toDocumentEntity()
            // Act
            val id = documentDb.insert(documentEntity)
            // Assert
            id shouldNotBe null
            id shouldBeGreaterThan 0L
        }

        it("should persist the document with the correct fields") {
            // Arrange
            val documentEntity = document().toDocumentEntity()
            // Act
            val id = documentDb.insert(documentEntity)
            // Assert
            val retrievedDocument = documentDb.getById(id)
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentEntity, id)
        }
    }
    describe("DocumentDb -> update") {
        it("should return a generated id") {
            // Arrange
            val documentEntity = document().toDocumentEntity()
            // Act
            val id = documentDb.insert(documentEntity)
            val updateddocumentEntity = documentEntity.copy(
                id = id,
                messageId = UUID.randomUUID(),
                status = DocumentStatus.COMPLETED,
                isRead = true,
            )
            documentDb.update(updateddocumentEntity)
            val retrievedDocument = documentDb.getById(id)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(updateddocumentEntity, id)
        }
    }

    describe("DocumentDb -> getById") {
        it("should return a documentEntity for the id") {
            // Arrange
            val documentEntity = document().toDocumentEntity()
            // Act
            val id = documentDb.insert(documentEntity)
            val retrievedDocument = documentDb.getById(id)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentEntity, id)
        }
    }

    describe("DocumentDb -> getByLinkId") {
        it("should return a documentEntity for the linktId") {
            // Arrange
            val documentEntity = document().toDocumentEntity()
            // Act
            val id = documentDb.insert(documentEntity)
            val retrievedDocument = documentDb.getByLinkId(documentEntity.linkId)
            // Assert
            retrievedDocument shouldNotBe null
            retrievedDocument?.assertExpected(documentEntity, id)
        }
    }
})

fun DocumentEntity.assertExpected(expected: DocumentEntity, id: Long) {
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
