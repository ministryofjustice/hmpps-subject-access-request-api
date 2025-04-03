package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
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
      .jsonPath("components.hmppsAuth.status").isEqualTo("UP")
      .componentHasStatusAndUrls("hmpps-document-management-api", "UP", 4040)
      .componentHasStatusAndUrls("prison-register", "UP", 9099)
      .componentHasStatusAndUrls("hmpps-external-users-api", "UP", 8084)
      .componentHasStatusAndUrls("nomis-user-roles-api", "UP", 8082)
      .componentHasStatusAndUrls("subject-access-requests-and-delius", "UP", 8083)
      .componentHasStatusAndUrls("hmpps-locations-inside-prison-api", "UP", 8085)
      .componentHasStatusAndUrls("hmpps-nomis-mapping-service", "UP", 8086)
      .jsonPath("components.sarServiceApis.status").isEqualTo("UP")
      .componentHasStatusAndUrls("my-dynamic-service", "UP", 8090, "sarServiceApis.details.")
      .componentHasStatusAndUrls("my-alt-dynamic-service", "UP", 8091, "sarServiceApis.details.")
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
      .componentHasStatusAndUrls("hmpps-document-management-api", "DOWN", 4040)
      .componentHasStatusAndUrls("prison-register", "DOWN", 9099)
      .componentHasStatusAndUrls("hmpps-external-users-api", "DOWN", 8084)
      .componentHasStatusAndUrls("nomis-user-roles-api", "DOWN", 8082)
      .componentHasStatusAndUrls("subject-access-requests-and-delius", "DOWN", 8083)
      .componentHasStatusAndUrls("hmpps-locations-inside-prison-api", "DOWN", 8085)
      .componentHasStatusAndUrls("hmpps-nomis-mapping-service", "DOWN", 8086)
      .jsonPath("components.sarServiceApis.status").isEqualTo("UP")
      .componentHasStatusAndUrls("my-dynamic-service", "DOWN", 8090, "sarServiceApis.details.")
      .componentHasStatusAndUrls("my-alt-dynamic-service", "DOWN", 8091, "sarServiceApis.details.")
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

  fun BodyContentSpec.componentHasStatusAndUrls(
    componentName: String,
    status: String,
    port: Int,
    pathPrefix: String = "",
  ): BodyContentSpec = this.jsonPath("components.$pathPrefix$componentName.status").isEqualTo(status)
    .jsonPath("components.$pathPrefix$componentName.details.healthUrl").isEqualTo("http://localhost:$port/health")
    .jsonPath("components.$pathPrefix$componentName.details.portalUrl")
    .isEqualTo("https://developer-portal.hmpps.service.justice.gov.uk/components/$componentName/environment/dev")
}
