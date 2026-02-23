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
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceConfigurationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService
import java.util.UUID

class ServicesControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var serviceConfigurationService: ServiceConfigurationService

  private val existingServiceConfig = ServiceConfiguration(
    serviceName = "existing-service",
    label = "hmpps-existing-service",
    url = "some value",
    category = PRISON,
    enabled = true,
    templateMigrated = false,
  )

  @BeforeEach
  fun setUp() {
    serviceConfigurationService.deleteByServiceName(existingServiceConfig.serviceName)
  }

  @AfterEach
  fun tearDown() {
    serviceConfigurationService.deleteByServiceName(existingServiceConfig.serviceName)
  }

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
    val services = serviceConfigurationService.getServiceConfigurationSanitised()
    assertThat(services).isNotNull

    val actual = webTestClient.get()
      .uri("/api/services")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList<ServiceInfo>()
      .returnResult()
      .responseBody

    assertThat(actual).isNotEmpty
    assertThat(actual).hasSize(services!!.size)

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
  inner class CreateServiceConfiguration {

    @ParameterizedTest
    @CsvSource(
      value = [
        "     |     |     |          |      |     | create service configuration requires non null non empty Name value",
        " ''  |     |     |          |      |     | create service configuration requires non null non empty Name value",
        " 'A' |     |     |          |      |     | create service configuration requires non null non empty Label value",
        " 'A' | ''  |     |          |      |     | create service configuration requires non null non empty Label value",
        " 'A' | 'B' |     |          |      |     | create service configuration requires non null non empty URL value",
        " 'A' | 'B' | ''  |          |      |     | create service configuration requires non null non empty URL value",
        " 'A' | 'B' | 'C' |          |      |     | create service configuration requires non null non empty Category value",
        " 'A' | 'B' | 'C' | ''       |      |     | create service configuration requires non null non empty Category value",
        " 'A' | 'B' | 'C' | 'D'      |      |     | create service configuration invalid Category value",
        " 'A' | 'B' | 'C' | 'PRISON' |      |     | create service configuration requires non null Enabled value",
        " 'A' | 'B' | 'C' | 'PRISON' | true |     | create service configuration requires non null Template Migrated value",
      ],
      delimiter = '|',
    )
    fun `should return status 400 when mandatory fields are missing`(
      name: String?,
      label: String?,
      url: String?,
      category: String?,
      enabled: Boolean?,
      templateMigrated: Boolean?,
      expectedErrorMessage: String?,
    ) {
      createServiceConfiguration(ServiceConfigurationEntity(name, label, url, category, enabled, templateMigrated))
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: $expectedErrorMessage")
    }

    @Test
    fun `should return status 400 when service already exists`() {
      serviceConfigurationService.createServiceConfiguration(existingServiceConfig)
      assertThat(serviceConfigurationService.getByServiceName(existingServiceConfig.serviceName)).isNotNull()

      createServiceConfiguration(
        body = ServiceConfigurationEntity(
          name = "existing-service",
          label = "hmpps-existing-service",
          url = "some value",
          category = "PRISON",
          enabled = true,
          templateMigrated = false,
        ),
      ).expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: Service configuration with name existing-service already exists")
    }

    @Test
    fun `should successfully create new service configuration`() {
      val actual = createServiceConfiguration(
        body = ServiceConfigurationEntity(
          name = "existing-service",
          label = "hmpps-existing-service",
          url = "some value",
          category = "PRISON",
          enabled = true,
          templateMigrated = false,
        ),
      )
        .expectStatus()
        .isCreated
        .expectBody<ServiceInfo>()
        .returnResult()

      assertThat(actual.responseBody).isNotNull
      assertThat(actual.responseBody!!.id).isNotNull
      assertThat(serviceConfigurationService.getById(actual.responseBody?.id!!)).isNotNull
    }
  }

  @Nested
  inner class UpdateServiceConfiguration {

    @Test
    fun `should return status 404 when service configuration does not exist`() {
      val entity = ServiceConfigurationEntity(
        name = "A",
        label = "B",
        url = "C",
        category = "PRISON",
        enabled = true,
        templateMigrated = true,
      )
      val id = UUID.randomUUID()

      putServiceConfiguration(id, entity)
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Service configuration service not found for id: $id")
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
    fun `should return status 400 when update contains invalid values`(
      name: String?,
      label: String?,
      url: String?,
      category: String?,
      enabled: Boolean?,
      templateMigrated: Boolean?,
      expectedErrorMessage: String?,
    ) {
      serviceConfigurationService.createServiceConfiguration(existingServiceConfig)
      assertThat(serviceConfigurationService.getById(existingServiceConfig.id)).isNotNull

      // Make update request
      putServiceConfiguration(
        id = existingServiceConfig.id,
        entity = ServiceConfigurationEntity(
          name = name,
          label = label,
          url = url,
          category = category,
          enabled = enabled,
          templateMigrated = templateMigrated,
        ),
      ).expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: $expectedErrorMessage")
    }

    @Test
    fun `should return status 400 when updated service name is used by an existing service configuration`() {
      val s1 = serviceConfigurationService.createServiceConfiguration(
        ServiceConfiguration(
          serviceName = "s1",
          label = "s1",
          url = "some value",
          category = PRISON,
          enabled = true,
          templateMigrated = false,
        ),
      )
      serviceConfigurationService.createServiceConfiguration(existingServiceConfig)

      // Make update request
      putServiceConfiguration(
        id = existingServiceConfig.id,
        entity = ServiceConfigurationEntity(
          name = s1.serviceName,
          label = existingServiceConfig.label,
          url = existingServiceConfig.url,
          category = PROBATION.name,
          enabled = true,
          templateMigrated = true,
        ),
      ).expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: serviceName 's1' is already in use by Service configuration ${s1.id}")

      val configAfterRequest = serviceConfigurationService.getById(existingServiceConfig.id)
      assertThat(configAfterRequest).isNotNull
      assertThat(configAfterRequest!!.serviceName).isEqualTo("existing-service")
    }

    @Test
    fun `should successfully update service configuration`() {
      serviceConfigurationService.createServiceConfiguration(existingServiceConfig)
      assertThat(serviceConfigurationService.getById(existingServiceConfig.id)).isNotNull

      putServiceConfiguration(
        id = existingServiceConfig.id,
        entity = ServiceConfigurationEntity(
          name = "X",
          label = "Y",
          url = "Z",
          category = PROBATION.name,
          enabled = false,
          templateMigrated = false,
        ),
      ).expectStatus()
        .isOk
        .expectBody(ServiceInfo::class.java)
        .returnResult()

      val latest = serviceConfigurationService.getById(existingServiceConfig.id)
      assertThat(latest).isNotNull
      assertThat(latest!!.serviceName).isEqualTo("X")
      assertThat(latest.label).isEqualTo("Y")
      assertThat(latest.url).isEqualTo("Z")
      assertThat(latest.category).isEqualTo(PROBATION)
      assertThat(latest.enabled).isFalse
      assertThat(latest.templateMigrated).isFalse
    }
  }

  private fun createServiceConfiguration(body: ServiceConfigurationEntity) = webTestClient.post()
    .uri("/api/services")
    .bodyValue(body)
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()

  private fun putServiceConfiguration(
    id: UUID,
    entity: ServiceConfigurationEntity,
  ) = webTestClient.put()
    .uri("/api/services/$id")
    .bodyValue(entity)
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()

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
