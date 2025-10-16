package no.nav.syfo.document.service

import document
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.OrganisasjonPrincipal
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.ereg.EregService
import no.nav.syfo.document.api.v1.DocumentType
import no.nav.syfo.ereg.client.Organisasjon

class ValidationServiceTest : DescribeSpec({
    val altinnTilgangerService = mockk<AltinnTilgangerService>()
    val eregService = mockk<EregService>()
    val validationService = ValidationService(altinnTilgangerService, eregService)

    val documentEntity = document().toDocumentEntity()

    describe("ValidationService") {
        describe("validateDocumentAccess") {
            context("when principal is BrukerPrincipal") {
                it("should validate Altinn tilgang") {
                    // Arrange
                    val brukerPrincipal = BrukerPrincipal("12345678901", "token")
                    coEvery { altinnTilgangerService.validateTilgangToOrganisasjon(any(), any(), any()) } returns Unit

                    // Act
                    validationService.validateDocumentAccess(brukerPrincipal, documentEntity)

                    // Assert
                    coVerify { altinnTilgangerService.validateTilgangToOrganisasjon(brukerPrincipal, "123456789", DocumentType.OPPFOLGINGSPLAN) }
                }
            }

            context("when principal is OrganisasjonPrincipal") {
                it("should validate Maskinporten tilgang") {
                    // Arrange
                    val organisasjonPrincipal = OrganisasjonPrincipal("0192:123456789", "token")
                    val organisasjon = Organisasjon(
                        organisasjonsnummer = "123456789",
                        inngaarIJuridiskEnheter = emptyList()
                    )
                    coEvery { eregService.getOrganization(any()) } returns organisasjon

                    // Act
                    validationService.validateDocumentAccess(organisasjonPrincipal, documentEntity)

                    // Assert - should not throw exception when orgnumbers match
                }
            }
        }

        describe("validateMaskinportenTilgang") {
            context("when orgnumber from token matches document orgnumber") {
                it("should allow access without checking parent organizations") {
                    // Arrange
                    val organisasjonPrincipal = OrganisasjonPrincipal("0192:123456789", "token")

                    // Act & Assert - should not throw exception
                    validationService.validateMaskinportenTilgang(organisasjonPrincipal, documentEntity)
                }
            }

            context("when orgnumber from token does not match document orgnumber") {
                context("and organization has parent organization with matching orgnumber") {
                    it("should allow access") {
                        // Arrange
                        val organisasjonPrincipal = OrganisasjonPrincipal("0192:123456789", "token")
                        val documentWithDifferentOrg = documentEntity.copy(orgnumber = "111222333")
                        val organisasjon = Organisasjon(
                            organisasjonsnummer = "111222333",
                            inngaarIJuridiskEnheter = listOf(
                                Organisasjon(organisasjonsnummer = "987654321")
                            )
                        )
                        coEvery { eregService.getOrganization("111222333") } returns organisasjon

                        // Act & Assert - should not throw exception
                        validationService.validateMaskinportenTilgang(organisasjonPrincipal, documentWithDifferentOrg)

                        coVerify { eregService.getOrganization("111222333") }
                    }
                }

                context("and organization has no parent organizations") {
                    it("should deny access") {
                        // Arrange
                        val organisasjonPrincipal = OrganisasjonPrincipal("0192:123456789", "token")
                        val documentWithDifferentOrg = documentEntity.copy(orgnumber = "111222333")
                        val organisasjon = Organisasjon(
                            organisasjonsnummer = "111222333",
                            inngaarIJuridiskEnheter = emptyList()
                        )
                        coEvery { eregService.getOrganization("111222333") } returns organisasjon

                        // Act & Assert
                        val exception = shouldThrow<ApiErrorException.ForbiddenException> {
                            validationService.validateMaskinportenTilgang(organisasjonPrincipal, documentWithDifferentOrg)
                        }
                        exception.message shouldBe "Access denied. Invalid organization."

                        coVerify { eregService.getOrganization("111222333") }
                    }
                }

                context("and organization has parent organizations but none match token orgnumber") {
                    it("should deny access") {
                        // Arrange
                        val organisasjonPrincipal = OrganisasjonPrincipal("0192:123456789", "token")
                        val documentWithDifferentOrg = documentEntity.copy(orgnumber = "111222333")
                        val organisasjon = Organisasjon(
                            organisasjonsnummer = "111222333",
                            inngaarIJuridiskEnheter = listOf(
                                Organisasjon(organisasjonsnummer = "555666777")
                            )
                        )
                        coEvery { eregService.getOrganization("111222333") } returns organisasjon

                        // Act & Assert
                        val exception = shouldThrow<ApiErrorException.ForbiddenException> {
                            validationService.validateMaskinportenTilgang(organisasjonPrincipal, documentWithDifferentOrg)
                        }
                        exception.message shouldBe "Access denied. Invalid organization."

                        coVerify { eregService.getOrganization("111222333") }
                    }
                }
            }
        }
    }
})
