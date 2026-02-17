package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateServiceConfigurationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.utils.ServiceConfigurationComparator

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
    val services = serviceConfigurationRepository.findAll()
      .sortedWith(ServiceConfigurationComparator())

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

  @Nested
  @Transactional
  inner class CreateServiceConfiguration {

    val existingServiceConfig = ServiceConfiguration(
      serviceName = "existing-service",
      label = "hmpps-existing-service",
      url = "some value",
      category = PRISON,
      enabled = true,
      templateMigrated = false,
    )

    @BeforeEach
    fun setUp() {
      serviceConfigurationRepository.deleteByServiceName(existingServiceConfig.serviceName)
    }

    @AfterEach
    fun tearDown() {
      serviceConfigurationRepository.deleteByServiceName(existingServiceConfig.serviceName)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "     |     |     |          |      |       | create service configuration requires non null non empty Name value",
        " ''  |     |     |          |      |       | create service configuration requires non null non empty Name value",
        " 'A' |     |     |          |      |       | create service configuration requires non null non empty Label value",
        " 'A' | ''  |     |          |      |       | create service configuration requires non null non empty Label value",
        " 'A' | 'B' |     |          |      |       | create service configuration requires non null non empty URL value",
        " 'A' | 'B' | ''  |          |      |       | create service configuration requires non null non empty URL value",
        " 'A' | 'B' | 'C' |          |      |       | create service configuration requires non null non empty Category value",
        " 'A' | 'B' | 'C' | ''       |      |       | create service configuration requires non null non empty Category value",
        " 'A' | 'B' | 'C' | 'D'      |      |       | create service configuration invalid Category value",
        " 'A' | 'B' | 'C' | 'PRISON' |      |       | create service configuration requires non null Enabled value",
        " 'A' | 'B' | 'C' | 'PRISON' | true |       | create service configuration requires non null Template Migrated value",
      ],
      delimiter = '|',
    )
    fun `should return status 400 when mandatory fields are missing`(
      name: String?,
      label: String?,
      url: String?,
      category: String?,
      enabled: String?,
      templateMigrated: String?,
      expectedErrorMessage: String?,
    ) {
      webTestClient.post()
        .uri("/api/services")
        .bodyValue(createJsonBody(name, label, url, category, enabled, templateMigrated))
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage").isEqualTo("Validation failure: $expectedErrorMessage")
    }

    @Test
    fun `should return status 400 when service already exists`() {
      assertThat(serviceConfigurationRepository.findByServiceName("existing-service")).isNull()

      serviceConfigurationRepository.saveAndFlush(
        ServiceConfiguration(
          serviceName = "existing-service",
          label = "hmpps-existing-service",
          url = "some value",
          category = PRISON,
          enabled = true,
          templateMigrated = false,
        ),
      )

      webTestClient.post()
        .uri("/api/services")
        .bodyValue(
          CreateServiceConfigurationEntity(
            name = "existing-service",
            label = "hmpps-existing-service",
            url = "some value",
            category = "PRISON",
            enabled = true,
            templateMigrated = false,
          ),
        )
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: Service configuration with name existing-service already exists")
    }

    @Test
    fun `should successfully create new service configuration`() {
      val actual = webTestClient.post()
        .uri("/api/services")
        .bodyValue(
          CreateServiceConfigurationEntity(
            name = "existing-service",
            label = "hmpps-existing-service",
            url = "some value",
            category = "PRISON",
            enabled = true,
            templateMigrated = false,
          ),
        )
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody<ServiceInfo>()
        .returnResult()

      assertThat(actual.responseBody).isNotNull
      assertThat(actual.responseBody!!.id).isNotNull
      assertThat(serviceConfigurationRepository.findByIdOrNull(actual.responseBody?.id!!)).isNotNull
    }

    private fun createJsonBody(
      name: String?,
      label: String?,
      url: String?,
      category: String?,
      enabled: String?,
      templateMigrated: String?,
    ): String = """{
        "name": ${name.getQuotedValueOrNull()}, 
        "label": ${label.getQuotedValueOrNull()}, 
        "url": ${url.getQuotedValueOrNull()}, 
        "category": ${category.getQuotedValueOrNull()},
        "enabled": ${enabled.takeIf { !it.isNullOrBlank() }?.toBoolean() ?: "null"},
        "templateMigrated": ${templateMigrated.takeIf { it.isNullOrBlank() }?.toBoolean() ?: "null"}
      }"""
      .trimMargin()
  }

  private fun String?.getQuotedValueOrNull(): String = this?.let { "\"$it\"" } ?: "null"

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
