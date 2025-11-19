package no.nav.syfo.document.api.v1

import DefaultOrganization
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import createMockToken
import defaultMocks
import document
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import no.nav.syfo.TestDB
import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.altinntilganger.client.FakeAltinnTilgangerClient
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.application.api.installStatusPages
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.ereg.EregService
import no.nav.syfo.ereg.client.FakeEregClient
import no.nav.syfo.registerApiV1
import no.nav.syfo.texas.MASKINPORTEN_ARKIVPORTEN_SCOPE
import no.nav.syfo.texas.client.TexasHttpClient
import organisasjon

class ExternalDocumentApiTest : DescribeSpec({
    val texasHttpClientMock = mockk<TexasHttpClient>()
    val DocumentDAOMock = mockk<DocumentDAO>()
    val fakeAltinnTilgangerClient = FakeAltinnTilgangerClient()
    val fakeEregClient = FakeEregClient()
    val eregService = EregService(fakeEregClient)
    val eregServiceSpy = spyk(eregService)
    val validationService = ValidationService(AltinnTilgangerService(fakeAltinnTilgangerClient), eregServiceSpy)
    val validationServiceSpy = spyk(validationService)
    val tokenXIssuer = "https://tokenx.nav.no"
    val idportenIssuer = "https://test.idporten.no"
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
                        texasHttpClient = texasHttpClientMock,
                        DocumentDAO = DocumentDAOMock,
                        validationService = validationServiceSpy
                    )
                }
            }
            fn(this)
        }
    }
    describe("GET /documents") {
        describe("Maskinporten token") {
            it("should return 200 OK for authorized token") {
                withTestApplication {
                    // Arrange
                    val document = document().toDocumentEntity()
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:${document.orgNumber}"
                        ),
                        scope = MASKINPORTEN_ARKIVPORTEN_SCOPE,
                    )
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(ident = document.orgNumber))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe document.contentType
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }

            it("should return 200 OK for authorized token from parent org unit") {
                withTestApplication {
                    // Arrange
                    val organization = organisasjon()
                    val document = document().copy(orgNumber = organization.organisasjonsnummer).toDocumentEntity()
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:${organization.inngaarIJuridiskEnheter!!.first().organisasjonsnummer}"
                        ),
                        scope = MASKINPORTEN_ARKIVPORTEN_SCOPE,
                    )
                    fakeEregClient.organisasjoner.put(document.orgNumber, organization)
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(ident = organization.inngaarIJuridiskEnheter.first().organisasjonsnummer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe document.contentType
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }

            it("should return 403 Forbidden for unauthorized token") {
                withTestApplication {
                    // Arrange
                    val nonMatchingOrgnumber = "999999999"
                    val organization = organisasjon()
                    val document = document().copy(orgNumber = organization.organisasjonsnummer).toDocumentEntity()
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:$nonMatchingOrgnumber" // Different orgNumber
                        ),
                        scope = MASKINPORTEN_ARKIVPORTEN_SCOPE,
                    )
                    fakeEregClient.organisasjoner.put(document.orgNumber, organization)
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(ident = nonMatchingOrgnumber))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Forbidden
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }
        }

        describe("TokenX token") {
            it("should return 200 OK for authorized token") {
                withTestApplication {
                    // Arrange
                    val document = document().toDocumentEntity()
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level4",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.add(callerPid to document.orgNumber)
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(callerPid, issuer = tokenXIssuer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe document.contentType
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }

            it("should return 403 Forbidden if token lacks Level4") {
                withTestApplication {
                    // Arrange
                    val document = document().toDocumentEntity()
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level3",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.add(callerPid to document.orgNumber)
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(callerPid, issuer = tokenXIssuer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Forbidden
                    coVerify(exactly = 0) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }

            it("should return 403 Forbidden when token user lacks altinn resource") {
                withTestApplication {
                    // Arrange
                    val document = document().toDocumentEntity()
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level4",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.clear()
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(callerPid, issuer = tokenXIssuer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Forbidden
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }
        }

        describe("Idporten token") {
            it("should return 200 OK for authorized token") {
                withTestApplication {
                    // Arrange
                    val document = document().toDocumentEntity()
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level4",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.add(callerPid to document.orgNumber)
                    coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns document
                    // Act
                    val response = client.get("api/v1/documents/${document.linkId}") {
                        bearerAuth(createMockToken(callerPid, issuer = idportenIssuer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe document.contentType
                    coVerify(exactly = 1) {
                        validationServiceSpy.validateDocumentAccess(any(), eq(document))
                    }
                }
            }

            describe("Not Found") {
                it("should return 404 Not found for unknown id") {
                    withTestApplication {
                        // Arrange
                        val document = document().toDocumentEntity()
                        coEvery { DocumentDAOMock.getByLinkId(eq(document.linkId)) } returns null
                        texasHttpClientMock.defaultMocks(
                            consumer = DefaultOrganization.copy(
                                ID = "0192:${document.orgNumber}"
                            ),
                            scope = MASKINPORTEN_ARKIVPORTEN_SCOPE,
                        )
                        // Act
                        val response = client.get("api/v1/documents/${document.linkId}") {
                            bearerAuth(createMockToken(ident = document.orgNumber))
                        }

                        // Assert
                        response.status shouldBe HttpStatusCode.NotFound
                        coVerify(exactly = 0) {
                            validationServiceSpy.validateDocumentAccess(any(), eq(document))
                        }
                    }
                }
            }
        }
    }
})
