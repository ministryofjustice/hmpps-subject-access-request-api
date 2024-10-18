package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase

class SubjectAccessRequestControllerIntTest : IntegrationTestBase() {

  @Test
  fun `User without ROLE_SAR_USER_ACCESS can't post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS_DENIED")))
      .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS"), name = "INTEGRATION_TEST_USER"))
      .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS"), name = "INTEGRATION_TEST_USER"))
      .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User without ROLE_SAR_USER_ACCESS can't get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS_DENIED")))
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }
}
