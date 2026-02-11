package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository

class ServicesControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

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
      .jsonPath("$[0].templateMigrated").isEqualTo(false)
      .jsonPath("$[0].category").isEqualTo(PRISON.name)
      .jsonPath("$[3].id").isNotEmpty
      .jsonPath("$[3].name").isEqualTo("hmpps-manage-adjudications-api")
      .jsonPath("$[3].label").isEqualTo("Adjudications")
      .jsonPath("$[3].url").isEqualTo("https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[3].templateMigrated").isEqualTo(false)
      .jsonPath("$[3].category").isEqualTo(PRISON.name)
      .jsonPath("$[13].id").isNotEmpty
      .jsonPath("$[13].name").isEqualTo("hmpps-hdc-api")
      .jsonPath("$[13].label").isEqualTo("Home detention curfew")
      .jsonPath("$[13].url").isEqualTo("https://hdc-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[13].templateMigrated").isEqualTo(false)
      .jsonPath("$[13].category").isEqualTo(PRISON.name)
      .jsonPath("$[25].id").isNotEmpty
      .jsonPath("$[25].name").isEqualTo("hmpps-support-additional-needs-api")
      .jsonPath("$[25].label").isEqualTo("Support for additional needs")
      .jsonPath("$[25].url").isEqualTo("https://support-for-additional-needs-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[25].templateMigrated").isEqualTo(false)
      .jsonPath("$[25].category").isEqualTo(PRISON.name)
      .jsonPath("$[30].id").isNotEmpty
      .jsonPath("$[30].name").isEqualTo("make-recall-decision-api")
      .jsonPath("$[30].label").isEqualTo("Consider a recall")
      .jsonPath("$[30].url").isEqualTo("https://make-recall-decision-api-dev.hmpps.service.justice.gov.uk")
      .jsonPath("$[30].templateMigrated").isEqualTo(false)
      .jsonPath("$[30].category").isEqualTo(PROBATION.name)
  }

  @Test
  fun `should return services in expected order`() {
    val services = serviceConfigurationRepository.findAllReportOrdering()

    val actual = webTestClient.get()
      .uri("/api/services")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList<ServiceInfo>()
      .returnResult()
      .responseBody

    assertThat(actual).isNotEmpty
    assertThat(actual).hasSize(services.size)

    val getErrorMessage: (Int, Any, Any) -> String = { index, actualValue, expectedValue ->
      "actual[$index].id $actualValue != expected[$index].id $expectedValue"
    }

    actual!!.forEachIndexed { index, info ->
      val expected = services[index]

      assertThat(info.id)
        .withFailMessage(getErrorMessage(index, info.id, expected.id))
        .isEqualTo(expected.id)

      assertThat(info.name)
        .withFailMessage(getErrorMessage(index, info.name, expected.serviceName))
        .isEqualTo(expected.serviceName)

      assertThat(info.label)
        .withFailMessage(getErrorMessage(index, info.label, expected.label))
        .isEqualTo(expected.label)

      assertThat(info.category)
        .withFailMessage(getErrorMessage(index, info.category, expected.category))
        .isEqualTo(expected.category)

      assertThat(info.url)
        .withFailMessage(getErrorMessage(index, info.url, expected.url))
        .isEqualTo(expected.url)

      assertThat(info.templateMigrated)
        .withFailMessage(getErrorMessage(index, info.templateMigrated, expected.templateMigrated))
        .isEqualTo(expected.templateMigrated)

      assertThat(info.enabled)
        .withFailMessage(getErrorMessage(index, info.enabled, expected.enabled))
        .isEqualTo(expected.enabled)
    }
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
