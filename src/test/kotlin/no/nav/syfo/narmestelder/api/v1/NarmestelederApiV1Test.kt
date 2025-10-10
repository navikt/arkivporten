import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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
import io.mockk.mockk
import io.mockk.spyk
import no.nav.syfo.aareg.client.FakeAaregClient
import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.altinntilganger.client.FakeAltinnTilgangerClient
import no.nav.syfo.application.api.ApiError
import no.nav.syfo.application.api.ErrorType
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.application.api.installStatusPages
import no.nav.syfo.application.auth.maskinportenIdToOrgnumber
import no.nav.syfo.registerApiV1
import no.nav.syfo.texas.MASKINPORTEN_NL_SCOPE
import no.nav.syfo.texas.client.TexasHttpClient

class armestelederApiV1Test : DescribeSpec({
    val texasHttpClientMock = mockk<TexasHttpClient>()
    val narmesteLederRelasjon = narmesteLederRelasjon()
    val fakeAaregClient = FakeAaregClient()
    val fakeAltinnTilgangerClient = FakeAltinnTilgangerClient()
    val altinnTilgangerServiceMock = AltinnTilgangerService(fakeAltinnTilgangerClient)
    val altinnTilgangerServiceSpy = spyk(altinnTilgangerServiceMock)
    val tokenXIssuer = "https://tokenx.nav.no"
    beforeTest {
        clearAllMocks()
        fakeAltinnTilgangerClient.usersWithAccess.clear()
        fakeAaregClient.arbeidsForholdForIdent.clear()
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
                    )
                }
            }
            fn(this)
        }
    }
    describe("POST /narmesteleder") {
        describe("Maskinporten token") {
            it("should return 202 Accepted for valid payload") {
                withTestApplication {
                    // Arrange
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:${narmesteLederRelasjon.organisasjonsnummer}"
                        ),
                        scope = MASKINPORTEN_NL_SCOPE,
                    )

                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon)
                        bearerAuth(createMockToken(narmesteLederRelasjon.organisasjonsnummer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Accepted
                }
            }

            it("should return 400 Bad Request for invalid payload") {
                withTestApplication {
                    // Arrange
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:${narmesteLederRelasjon.organisasjonsnummer}"
                        ),
                        scope = MASKINPORTEN_NL_SCOPE,
                    )
                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody("""{ "navn": "Ola Nordmann" }""")
                        bearerAuth(createMockToken(maskinportenIdToOrgnumber(DefaultOrganization.ID)))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.BadRequest
                    response.body<ApiError>().type shouldBe ErrorType.BAD_REQUEST
                }
            }

            it("should return 401 unauthorized for missing token") {
                withTestApplication {
                    // Arrange
                    texasHttpClientMock.defaultMocks()
                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon())
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Unauthorized
                    response.body<ApiError>().type shouldBe ErrorType.AUTHORIZATION_ERROR
                }
            }

            it("should return 401 unauthorized for missing valid maskinporten scope") {
                withTestApplication {
                    // Arrange
                    texasHttpClientMock.defaultMocks(
                        consumer = DefaultOrganization.copy(
                            ID = "0192:${narmesteLederRelasjon.organisasjonsnummer}"
                        ),
                        scope = "invalid-scope",
                    )
                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon())
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Unauthorized
                    response.body<ApiError>().type shouldBe ErrorType.AUTHORIZATION_ERROR
                }
            }

            it("should return 401 unauthorized for invalid token issuer") {
                withTestApplication {
                    // Arrange
                    texasHttpClientMock.defaultMocks()
                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon())
                        bearerAuth(createMockToken(ident = "", issuer = "invalid"))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Unauthorized
                    response.body<ApiError>().type shouldBe ErrorType.AUTHORIZATION_ERROR
                }
            }
        }
        describe("TokenX token") {
            it("should return 202 Accepted for valid payload") {
                withTestApplication {
                    // Arrange
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level4",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.add(callerPid to narmesteLederRelasjon.organisasjonsnummer)
                    fakeAaregClient.arbeidsForholdForIdent.put(
                        narmesteLederRelasjon.sykmeldtFnr,
                        listOf(narmesteLederRelasjon.organisasjonsnummer to narmesteLederRelasjon.organisasjonsnummer)
                    )

                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon)
                        bearerAuth(createMockToken(callerPid, issuer = tokenXIssuer))
                    }
                    // Assert
                    response.status shouldBe HttpStatusCode.Accepted
                }
            }

            it("should return 403 when caller lacks Level4") {
                withTestApplication {
                    // Arrange
                    val callerPid = "11223344556"
                    texasHttpClientMock.defaultMocks(
                        acr = "Level3",
                        pid = callerPid
                    )
                    fakeAltinnTilgangerClient.usersWithAccess.add(callerPid to narmesteLederRelasjon.organisasjonsnummer)
                    // Act
                    val response = client.post("/api/v1/narmesteleder") {
                        contentType(ContentType.Application.Json)
                        setBody(narmesteLederRelasjon)
                        bearerAuth(createMockToken(callerPid, issuer = tokenXIssuer))
                    }

                    // Assert
                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }
        }
    }

    describe("POST /narmesteleder/avkreft") {
        it("should return 202 Accepted for valid payload") {
            val narmesteLederAvkreft = narmesteLederAvkreft()
            withTestApplication {
                // Arrange
                texasHttpClientMock.defaultMocks(
                    consumer = DefaultOrganization.copy(
                        ID = "0192:${narmesteLederAvkreft.organisasjonsnummer}"
                    ),
                    scope = MASKINPORTEN_NL_SCOPE,
                )
                val narmesteLederAvkreft = narmesteLederAvkreft
                fakeAaregClient.arbeidsForholdForIdent.clear()
                fakeAaregClient.arbeidsForholdForIdent.put(
                    narmesteLederAvkreft.sykmeldtFnr,
                    listOf(narmesteLederAvkreft.organisasjonsnummer to narmesteLederRelasjon.organisasjonsnummer)
                )
                // Act
                val response = client.post("/api/v1/narmesteleder/avkreft") {
                    contentType(ContentType.Application.Json)
                    setBody(narmesteLederAvkreft)
                    bearerAuth(createMockToken(maskinportenIdToOrgnumber(DefaultOrganization.ID)))
                }

                // Assert
                response.status shouldBe HttpStatusCode.Accepted
            }
        }


        it("should return 400 Bad Request for invalid payload") {
            withTestApplication {
                // Arrange
                texasHttpClientMock.defaultMocks(
                    consumer = DefaultOrganization.copy(
                        ID = "0192:${narmesteLederRelasjon.organisasjonsnummer}"
                    ),
                    scope = MASKINPORTEN_NL_SCOPE,
                )
                // Act
                val response = client.post("/api/v1/narmesteleder/avkreft") {
                    contentType(ContentType.Application.Json)
                    setBody("""{ "navn": "Ola Nordmann" }""")
                    bearerAuth(createMockToken(maskinportenIdToOrgnumber(DefaultOrganization.ID)))
                }

                // Assert
                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ApiError>().type shouldBe ErrorType.BAD_REQUEST
            }
        }
    }
})
