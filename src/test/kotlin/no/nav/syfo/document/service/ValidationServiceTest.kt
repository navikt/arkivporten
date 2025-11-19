package no.nav.syfo.document.service

import document
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.application.auth.OrganisasjonPrincipal
import no.nav.syfo.application.exception.ApiErrorException
import no.nav.syfo.ereg.EregService
import organisasjon

class ValidationServiceTest : DescribeSpec({
    val altinnTilgangerService = mockk<AltinnTilgangerService>()
    val eregService = mockk<EregService>()
    val validationService = ValidationService(altinnTilgangerService, eregService)

    val documentEntity = document().toDocumentEntity()
    beforeTest {
        clearAllMocks()
    }

    describe("ValidationService") {
        describe("validateDocumentAccess") {
            context("when principal is BrukerPrincipal") {
                it("should validate Altinn tilgang and not throw for valid access") {
                    // Arrange
                    val brukerPrincipal = BrukerPrincipal("12345678901", "token")
                    coEvery { altinnTilgangerService.validateTilgangToOrganisasjon(any(), any(), any()) } returns Unit

                    // Act
                    validationService.validateDocumentAccess(brukerPrincipal, documentEntity)

                    // Assert
                    coVerify(exactly = 1) {
                        altinnTilgangerService.validateTilgangToOrganisasjon(
                            any(),
                            any(),
                            any()
                        )
                    }
                    coVerify(exactly = 0) {
                        eregService.getOrganization(any())
                    }
                }

                it("should validate Altinn tilgang and pass through exception from AltinnTilgangerService") {
                    // Arrange
                    val brukerPrincipal = BrukerPrincipal("12345678901", "token")
                    coEvery {
                        altinnTilgangerService.validateTilgangToOrganisasjon(
                            any(),
                            any(),
                            any()
                        )
                    } throws ApiErrorException.ForbiddenException("No access")

                    // Act
                    shouldThrow<ApiErrorException.ForbiddenException> {
                        validationService.validateDocumentAccess(brukerPrincipal, documentEntity)
                    }
                    // Assert
                    coVerify(exactly = 1) {
                        altinnTilgangerService.validateTilgangToOrganisasjon(
                            any(),
                            any(),
                            any()
                        )
                    }
                    coVerify(exactly = 0) {
                        eregService.getOrganization(any())
                    }
                }


            }
        }

        describe("validateMaskinportenTilgang") {
            context("when orgnumber from token matches document orgnumber") {
                it("should allow access without checking ereg when Principal matches document orgnumber") {
                    // Arrange
                    val organisasjonPrincipal = OrganisasjonPrincipal("0192:${documentEntity.orgNumber}", "token")

                    // Act & Assert - should not throw exception
                    validationService.validateMaskinportenTilgang(organisasjonPrincipal, documentEntity)
                    coVerify(exactly = 0) {
                        eregService.getOrganization(any())
                    }
                    coVerify(exactly = 0) {
                        altinnTilgangerService.validateTilgangToOrganisasjon(any(), any(), any())
                    }
                }
            }

            context("when orgnumber from token does not match document orgnumber") {
                context("and organization has parent organization with matching orgnumber") {
                    it("should allow access") {
                        // Arrange
                        val organization = organisasjon()
                        val entity = documentEntity.copy(orgNumber = organization.organisasjonsnummer)

                        val organisasjonPrincipal = OrganisasjonPrincipal(
                            "0192:${organization.inngaarIJuridiskEnheter!!.first().organisasjonsnummer}",
                            "token"
                        )
                        coEvery { eregService.getOrganization(entity.orgNumber) } returns organization

                        // Act & Assert - should not throw exception
                        validationService.validateMaskinportenTilgang(
                            organisasjonPrincipal,
                            entity,
                        )

                        coVerify(exactly = 1) {
                            eregService.getOrganization(eq(entity.orgNumber))
                        }
                    }
                }

                context("and organization has no parent organizations") {
                    it("should deny access") {
                        // Arrange
                        val organization = organisasjon()
                        val entity = documentEntity.copy(orgNumber = organization.organisasjonsnummer)

                        val organisasjonPrincipal = OrganisasjonPrincipal(
                            "0192:${organization.inngaarIJuridiskEnheter!!.first().organisasjonsnummer}",
                            "token"
                        )
                        coEvery { eregService.getOrganization(entity.orgNumber) } returns organization.copy(
                            inngaarIJuridiskEnheter = null
                        )

                        // Act & Assert
                        val exception = shouldThrow<ApiErrorException.ForbiddenException> {
                            validationService.validateMaskinportenTilgang(
                                organisasjonPrincipal, entity
                            )
                        }
                        coVerify { eregService.getOrganization(entity.orgNumber) }
                    }
                }

                context("and organization has parent organizations but none match token orgnumber") {
                    it("should deny access") {
                        // Arrange
                        val organization = organisasjon()
                        val entity = documentEntity.copy(orgNumber = organization.organisasjonsnummer)

                        val organisasjonPrincipal = OrganisasjonPrincipal(
                            "0192:123456789",
                            "token"
                        )
                        coEvery { eregService.getOrganization(entity.orgNumber) } returns organization

                        // Act & Assert
                        val exception = shouldThrow<ApiErrorException.ForbiddenException> {
                            validationService.validateMaskinportenTilgang(
                                organisasjonPrincipal, entity
                            )
                        }
                        exception.message shouldBe "Access denied. Invalid organization."

                        coVerify { eregService.getOrganization(eq(entity.orgNumber)) }
                    }
                }
            }
        }
    }
})
