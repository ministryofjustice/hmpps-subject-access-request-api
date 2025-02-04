package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase

class ServicesControllerIntTest : IntegrationTestBase() {

  @Test
  fun `should get status unauthorised when no auth token is provided`() {
    webTestClient.get()
      .uri("/api/services")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should get status forbidden when token does not have the required roles`() {
    webTestClient.get()
      .uri("/api/services")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("roles")
  fun `should return service list when auth token with appropriate role provided`(role: String) {
    webTestClient.get()
      .uri("/api/services")
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").isArray
      .jsonPath("$.length()").isEqualTo(26)
      .jsonPath("$[0].id").isNotEmpty
      .jsonPath("$[0].name").isEqualTo("keyworker-api")
      .jsonPath("$[0].label").isEqualTo("Keyworker")
      .jsonPath("$[0].url").isEqualTo("https://keyworker-api-dev.prison.service.justice.gov.uk")
      .jsonPath("$[0].order").isEqualTo(1)
      .jsonPath("$[1].id").isNotEmpty
      .jsonPath("$[1].name").isEqualTo("offender-case-notes")
      .jsonPath("$[1].label").isEqualTo("Sensitive Case Notes")
      .jsonPath("$[1].url").isEqualTo("https://dev.offender-case-notes.service.justice.gov.uk")
      .jsonPath("$[1].order").isEqualTo(2)
      .jsonPath("$[13].id").isNotEmpty
      .jsonPath("$[13].name").isEqualTo("make-recall-decision-api")
      .jsonPath("$[13].label").isEqualTo("Consider a Recall")
      .jsonPath("$[13].url").isEqualTo("https://make-recall-decision-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[13].order").isEqualTo(14)
      .jsonPath("$[25].id").isNotEmpty
      .jsonPath("$[25].name").isEqualTo("G3")
      .jsonPath("$[25].label").isEqualTo("G3")
      .jsonPath("$[25].url").isEqualTo("G3")
      .jsonPath("$[25].order").isEqualTo(26)
  }

  companion object {
    @JvmStatic
    fun roles(): List<String> = listOf(
      "ROLE_SAR_USER_ACCESS",
      "ROLE_SAR_DATA_ACCESS",
      "ROLE_SAR_SUPPORT",
    )
  }
}
