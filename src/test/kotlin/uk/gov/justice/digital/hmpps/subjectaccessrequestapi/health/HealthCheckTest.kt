package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionHealthStatusRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService

class HealthCheckTest : IntegrationTestBase() {

  @Autowired
  private lateinit var serviceConfigurationService: ServiceConfigurationService

  @Autowired
  private lateinit var templateVersionHealthRepository: TemplateVersionHealthStatusRepository

  @BeforeEach
  fun setup() {
    templateVersionHealthRepository.deleteAll()
  }

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)
    setTemplateHealthStatus("my-alt-dynamic-service", HealthStatusType.HEALTHY)

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
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-dynamic-service",
        "UP",
        8090,
        "sarServiceApis.details.",
        HealthStatusType.NOT_MIGRATED,
      )
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-alt-dynamic-service",
        "UP",
        8091,
        "sarServiceApis.details.",
        HealthStatusType.HEALTHY,
      )
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(503)
    setTemplateHealthStatus("my-alt-dynamic-service", HealthStatusType.UNHEALTHY)
    setTemplateHealthStatus("my-dynamic-service", HealthStatusType.HEALTHY)

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
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-dynamic-service",
        "DOWN",
        8090,
        "sarServiceApis.details.",
        HealthStatusType.HEALTHY,
      )
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-alt-dynamic-service",
        "DOWN",
        8091,
        "sarServiceApis.details.",
        HealthStatusType.UNHEALTHY,
      )
  }

  @Test
  fun `Health page reports status service UP even if template health status is unhealthy`() {
    stubPingWithResponse(200)
    setTemplateHealthStatus("my-alt-dynamic-service", HealthStatusType.UNHEALTHY)
    setTemplateHealthStatus("my-dynamic-service", HealthStatusType.UNHEALTHY)

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
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-dynamic-service",
        "UP",
        8090,
        "sarServiceApis.details.",
        HealthStatusType.UNHEALTHY,
      )
      .componentHasStatusAndUrlsAndTemplateHealthStatue(
        "my-alt-dynamic-service",
        "UP",
        8091,
        "sarServiceApis.details.",
        HealthStatusType.UNHEALTHY,
      )
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
    .jsonPath("components.$pathPrefix$componentName.details.healthUrl")
    .isEqualTo("http://localhost:$port/health")
    .jsonPath("components.$pathPrefix$componentName.details.portalUrl")
    .isEqualTo("https://developer-portal.hmpps.service.justice.gov.uk/components/$componentName/environment/dev")

  fun BodyContentSpec.componentHasStatusAndUrlsAndTemplateHealthStatue(
    componentName: String,
    status: String,
    port: Int,
    pathPrefix: String = "",
    templateHealthStatus: HealthStatusType = HealthStatusType.HEALTHY,
  ): BodyContentSpec = this.jsonPath("components.$pathPrefix$componentName.status").isEqualTo(status)
    .jsonPath("components.$pathPrefix$componentName.details.healthUrl")
    .isEqualTo("http://localhost:$port/health")
    .jsonPath("components.$pathPrefix$componentName.details.portalUrl")
    .isEqualTo("https://developer-portal.hmpps.service.justice.gov.uk/components/$componentName/environment/dev")
    .jsonPath("components.$pathPrefix$componentName.details.templateHealthStatus")
    .isEqualTo(templateHealthStatus.name)

  private fun setTemplateHealthStatus(serviceName: String, status: HealthStatusType) {
    val serviceConfig = serviceConfigurationService.getByServiceName(serviceName)
    assertThat(serviceConfig).isNotNull

    templateVersionHealthRepository.saveAndFlush(
      TemplateVersionHealthStatus(
        serviceConfiguration = serviceConfig!!,
        status = status,
      ),
    )
  }
}
