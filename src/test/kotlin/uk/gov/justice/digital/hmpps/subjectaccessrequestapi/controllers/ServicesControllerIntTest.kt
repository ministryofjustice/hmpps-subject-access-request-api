package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION

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
      .jsonPath("$.length()").isEqualTo(33)
      .jsonPath("$[0].id").isNotEmpty
      .jsonPath("$[0].name").isEqualTo("G1")
      .jsonPath("$[0].label").isEqualTo("G1")
      .jsonPath("$[0].url").isEqualTo("G1")
      .jsonPath("$[0].order").isEqualTo(1)
      .jsonPath("$[0].templateMigrated").isEqualTo(false)
      .jsonPath("$[0].category").isEqualTo(PRISON.name)
      .jsonPath("$[3].id").isNotEmpty
      .jsonPath("$[3].name").isEqualTo("hmpps-manage-adjudications-api")
      .jsonPath("$[3].label").isEqualTo("Adjudications")
      .jsonPath("$[3].url").isEqualTo("https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[3].order").isEqualTo(4)
      .jsonPath("$[3].templateMigrated").isEqualTo(false)
      .jsonPath("$[3].category").isEqualTo(PRISON.name)
      .jsonPath("$[13].id").isNotEmpty
      .jsonPath("$[13].name").isEqualTo("launchpad-auth")
      .jsonPath("$[13].label").isEqualTo("Launchpad")
      .jsonPath("$[13].url").isEqualTo("https://launchpad-auth-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[13].order").isEqualTo(14)
      .jsonPath("$[13].templateMigrated").isEqualTo(false)
      .jsonPath("$[13].category").isEqualTo(PRISON.name)
      .jsonPath("$[25].id").isNotEmpty
      .jsonPath("$[25].name").isEqualTo("hmpps-accredited-programmes-api")
      .jsonPath("$[25].label").isEqualTo("Accredited programmes")
      .jsonPath("$[25].url").isEqualTo("https://accredited-programmes-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[25].order").isEqualTo(26)
      .jsonPath("$[25].templateMigrated").isEqualTo(false)
      .jsonPath("$[25].category").isEqualTo(PROBATION.name)
  }

  companion object {
    @JvmStatic
    fun roles(): List<String> = listOf(
      "ROLE_SAR_USER_ACCESS",
      "ROLE_SAR_DATA_ACCESS",
      "ROLE_SAR_SUPPORT",
      "ROLE_SAR_REGISTER_TEMPLATE",
    )
  }
}
