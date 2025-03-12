package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(503)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.hmppsAuth.status").isEqualTo("DOWN")
      .jsonPath("components.externalUserApi.status").isEqualTo("DOWN")
      .jsonPath("components.prisonRegister.status").isEqualTo("DOWN")
      .jsonPath("components.nomisUserRolesApi.status").isEqualTo("DOWN")
      .jsonPath("components.sarAndDeliusApi.status").isEqualTo("DOWN")
      .jsonPath("components.locationsApi.status").isEqualTo("DOWN")
      .jsonPath("components.nomisMappingsApi.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
