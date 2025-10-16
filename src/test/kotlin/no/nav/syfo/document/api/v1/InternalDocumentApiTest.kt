package no.nav.syfo.document.api.v1

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import createMockToken
import defaultMocks
import document
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.TestDB
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.application.api.installStatusPages
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.registerApiV1
import no.nav.syfo.texas.client.TexasHttpClient

class InternalDocumentApiTest : DescribeSpec({
    val texasHttpClientMock = mockk<TexasHttpClient>()
    val documentDAOMock = mockk<DocumentDAO>()
    beforeTest {
        clearAllMocks()
        TestDB.clearAllData()
    }
    fun withTestApplication(
        fn: suspend ApplicationTestBuilder.() -> Unit
    ) {
        testApplication {
            this.client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
            }
            application {
                installContentNegotiation()
                installStatusPages()
                routing {
                    registerApiV1(
                        texasHttpClientMock,
                        documentDAOMock
                    )
                }
            }
            fn(this)
        }
    }
    describe("POST /documents") {
        it("should return 200 OK for valid payload") {
            withTestApplication {
                // Arrange
                coEvery { documentDAOMock.insert(any()) } returns 1L
                texasHttpClientMock.defaultMocks()
                // Act
                val response = client.post("/internal/api/v1/documents") {
                    contentType(ContentType.Application.Json)
                    setBody(document())
                    bearerAuth(createMockToken(ident = "", issuer = "https://test.azuread.microsoft.com"))
                }

                // Assert
                response.status shouldBe HttpStatusCode.OK
                // Verify that the document was inserted into the database
                verify(exactly = 1) {
                    documentDAOMock.insert(any())
                }
            }
        }

        it("should return 400 on invalid") {
            withTestApplication {
                // Arrange
                texasHttpClientMock.defaultMocks()
                // Act
                val response = client.post("/internal/api/v1/documents") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"invalid": "payload"}""")
                    bearerAuth(createMockToken(ident = "", issuer = "https://test.azuread.microsoft.com"))
                }

                // Assert
                response.status shouldBe HttpStatusCode.BadRequest
                // Verify that the document was inserted into the database
                verify(exactly = 0) {
                    documentDAOMock.insert(any())
                }
            }
        }
        it("should return 500 on db write error") {
            withTestApplication {
                // Arrange
                texasHttpClientMock.defaultMocks()
                coEvery { documentDAOMock.insert(any()) } throws RuntimeException("DB error")
                // Act
                val response = client.post("/internal/api/v1/documents") {
                    contentType(ContentType.Application.Json)
                    setBody(document())
                    bearerAuth(createMockToken(ident = "", issuer = "https://test.azuread.microsoft.com"))
                }

                // Assert
                response.status shouldBe HttpStatusCode.InternalServerError
                response.body<String>() shouldNotContain "DB error"
                // Verify that the document was inserted into the database
                verify(exactly = 1) {
                    documentDAOMock.insert(any())
                }
            }
        }
    }
})
