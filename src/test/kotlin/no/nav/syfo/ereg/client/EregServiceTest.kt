package no.nav.syfo.ereg.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.ereg.EregService
import no.nav.syfo.application.exception.ApiErrorException

class EregServiceTest : DescribeSpec({
//    describe("findOrgNumbersByPersonIdent") {
//        it("should extract map of orgnumbers from AaregArbeidsforholdOversikt") {
//            // Arrange
//            val fakeEregClient = FakeEregClient()
//            val fnr = "12345678901"
//            val virksomhet = "987654321"
//            val juridiskOrgnummer = "123456789"
//            fakeEregClient.organisasjoner.clear()
//            fakeEregClient.organisasjoner.put(fnr, listOf(virksomhet to juridiskOrgnummer))
//            val service = EregService(fakeEregClient)
//            val expected = fakeEregClient.getOrganisasjon(juridiskOrgnummer)
//            // Act
//            val result = service.getOrganization(expected.key)
//
//            // Assert
//
//            result.size shouldBe 1
//            result.entries.first().key shouldBe virksomhet
//            result.entries.first().value shouldBe juridiskOrgnummer
//        }
//    }
//    it("Should convert AaregClientException to ApiErrorException") {
//        // Arrange
//        val fnr = "12345678901"
//        val fakeEregClient = FakeEregClient()
//        fakeEregClient.setFailure(EregClientException("Forced failure", Exception()))
//        val service = EregService(fakeEregClient)
//
//        // Act
//        // Assert
//        shouldThrow<ApiErrorException.InternalServerErrorException> {
//            service.getOrganization(fnr)
//        }
//    }
})
