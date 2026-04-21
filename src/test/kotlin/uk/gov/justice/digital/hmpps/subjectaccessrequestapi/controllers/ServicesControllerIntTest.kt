package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceConfigurationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService
import uk.gov.service.notify.NotificationClientApi
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.UUID

@TestPropertySource(
  properties = [
    "application.notify.suspend-product.email-addresses=suspend@test.com",
    "application.notify.unsuspend-product.email-addresses=unsuspend@test.com",
  ],
)
@Import(ServicesControllerIntTest.TestClockConfig::class)
class ServicesControllerIntTest : IntegrationTestBase() {

  @TestConfiguration
  class TestClockConfig {
    @Bean
    @Primary
    fun testClock(): Clock = Clock.fixed(Instant.parse("2026-03-20T09:45:23Z"), UTC)
  }

  @MockitoBean
  private lateinit var notificationClient: NotificationClientApi

  @Autowired
  private lateinit var serviceConfigurationService: ServiceConfigurationService

  private val s1 = ServiceConfiguration(
    serviceName = "S1",
    label = "A",
    url = "B",
    category = PRISON,
    enabled = true,
    templateMigrated = true,
  )

  private val s2 = ServiceConfiguration(
    serviceName = "S2",
    label = "X",
    url = "Y",
    category = PRISON,
    enabled = true,
    templateMigrated = true,
  )

  private val serviceNamesToCleanUp = listOf(s1.serviceName, s2.serviceName, "X")

  @BeforeEach
  fun setUp() {
    serviceNamesToCleanUp.forEach { serviceConfigurationService.deleteByServiceName(it) }
    Mockito.reset(notificationClient)
  }

  @AfterEach
  fun tearDown() {
    serviceNamesToCleanUp.forEach { serviceConfigurationService.deleteByServiceName(it) }
  }

  @Nested
  inner class GetServices {

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
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#roles")
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
        .jsonPath("$[0].enabled").isEqualTo(true)
        .jsonPath("$[0].suspended").isEqualTo(false)
        .jsonPath("$[0].suspendedAt").isEmpty
        .jsonPath("$[3].id").isNotEmpty
        .jsonPath("$[3].name").isEqualTo("hmpps-manage-adjudications-api")
        .jsonPath("$[3].label").isEqualTo("Adjudications")
        .jsonPath("$[3].url").isEqualTo("http://localhost:8090")
        .jsonPath("$[3].templateMigrated").isEqualTo(false)
        .jsonPath("$[3].category").isEqualTo(PRISON.name)
        .jsonPath("$[3].enabled").isEqualTo(true)
        .jsonPath("$[3].suspended").isEqualTo(false)
        .jsonPath("$[3].suspendedAt").isEmpty
        .jsonPath("$[13].id").isNotEmpty
        .jsonPath("$[13].name").isEqualTo("hmpps-hdc-api")
        .jsonPath("$[13].label").isEqualTo("Home detention curfew")
        .jsonPath("$[13].url").isEqualTo("http://localhost:8090")
        .jsonPath("$[13].templateMigrated").isEqualTo(false)
        .jsonPath("$[13].category").isEqualTo(PRISON.name)
        .jsonPath("$[13].enabled").isEqualTo(true)
        .jsonPath("$[13].suspended").isEqualTo(false)
        .jsonPath("$[13].suspendedAt").isEmpty
        .jsonPath("$[25].id").isNotEmpty
        .jsonPath("$[25].name").isEqualTo("hmpps-support-additional-needs-api")
        .jsonPath("$[25].label").isEqualTo("Support for additional needs")
        .jsonPath("$[25].url").isEqualTo("http://localhost:8090")
        .jsonPath("$[25].templateMigrated").isEqualTo(false)
        .jsonPath("$[25].category").isEqualTo(PRISON.name)
        .jsonPath("$[25].enabled").isEqualTo(true)
        .jsonPath("$[25].suspended").isEqualTo(false)
        .jsonPath("$[25].suspendedAt").isEmpty
        .jsonPath("$[30].id").isNotEmpty
        .jsonPath("$[30].name").isEqualTo("make-recall-decision-api")
        .jsonPath("$[30].label").isEqualTo("Consider a recall")
        .jsonPath("$[30].url").isEqualTo("http://localhost:8090")
        .jsonPath("$[30].templateMigrated").isEqualTo(false)
        .jsonPath("$[30].category").isEqualTo(PROBATION.name)
        .jsonPath("$[30].enabled").isEqualTo(true)
        .jsonPath("$[30].suspended").isEqualTo(false)
        .jsonPath("$[30].suspendedAt").isEmpty
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

        assertThat(info.suspended)
          .withFailMessage(getErrorMessage(index, info.suspended, expected.suspended))
          .isEqualTo(expected.suspended)

        assertThat(info.suspendedAt)
          .withFailMessage(getErrorMessage(index, info.suspendedAt ?: "null", expected.suspendedAt ?: "null"))
          .isEqualTo(expected.suspendedAt)
      }
    }
  }

  @Nested
  inner class CreateServiceConfiguration {

    @Test
    fun `should get status unauthorised when no auth token is provided`() {
      webTestClient.post()
        .uri("/api/services")
        .bodyValue(s1)
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should get status forbidden when token does not have the required roles`() {
      webTestClient.post()
        .uri("/api/services")
        .bodyValue(s1)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

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
      createServiceConfiguration(
        body = ServiceConfigurationEntity(
          name,
          label,
          url,
          category,
          enabled,
          templateMigrated,
        ),
        roles = createAndUpdateServiceRoles(),
      )
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: $expectedErrorMessage")
    }

    @Test
    fun `should return status 400 when service already exists`() {
      ensureServiceConfigurationExists(s1)

      createServiceConfiguration(
        body = s1.toEntity(),
        roles = createAndUpdateServiceRoles(),
      )
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: Service configuration with name S1 already exists")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should successfully create new service configuration`(
      role: String,
    ) {
      val actual = createServiceConfiguration(
        body = s1.toEntity(),
        roles = listOf(role),
      )
        .expectStatus()
        .isCreated
        .expectBody<ServiceInfo>()
        .returnResult()

      assertThat(actual.responseBody).isNotNull
      assertThat(actual.responseBody!!.id).isNotNull

      val saved = serviceConfigurationService.getById(actual.responseBody?.id!!)
      assertThat(saved).isNotNull
      assertServiceConfigurationIsNotSuspended(saved!!.id)
    }
  }

  @Nested
  inner class UpdateServiceConfiguration {

    @Test
    fun `should get status unauthorised when no auth token is provided`() {
      webTestClient.put()
        .uri("/api/services/${s1.id}")
        .bodyValue(s1)
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should get status forbidden when token does not have the required roles`() {
      webTestClient.put()
        .uri("/api/services/${s1.id}")
        .bodyValue(s1)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return status 404 when service configuration does not exist`() {
      val id = UUID.randomUUID()

      putServiceConfiguration(
        id = id,
        body = s2.toEntity(),
        roles = listOf("ROLE_SAR_SUPPORT"),
      )
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
      serviceConfigurationService.createServiceConfiguration(s1)
      assertThat(serviceConfigurationService.getById(s1.id)).isNotNull

      // Make update request
      putServiceConfiguration(
        id = s1.id,
        body = ServiceConfigurationEntity(
          name = name,
          label = label,
          url = url,
          category = category,
          enabled = enabled,
          templateMigrated = templateMigrated,
        ),
        roles = listOf("ROLE_SAR_SUPPORT"),
      ).expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: $expectedErrorMessage")
    }

    @Test
    fun `should return status 400 when updated service name is used by an existing service configuration`() {
      serviceConfigurationService.createServiceConfiguration(s1)
      serviceConfigurationService.createServiceConfiguration(s2)

      // Make update request
      putServiceConfiguration(
        id = s1.id,
        body = ServiceConfigurationEntity(
          name = s2.serviceName,
          label = s1.label,
          url = s1.url,
          category = PROBATION.name,
          enabled = true,
          templateMigrated = true,
        ),
        roles = listOf("ROLE_SAR_SUPPORT"),
      ).expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure: serviceName 'S2' is already in use by Service configuration ${s2.id}")

      val configAfterRequest = serviceConfigurationService.getById(s1.id)
      assertThat(configAfterRequest).isNotNull
      assertThat(configAfterRequest!!.serviceName).isEqualTo(s1.serviceName)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should successfully update service configuration`(role: String) {
      ensureServiceConfigurationExists(s1)

      putServiceConfiguration(
        id = s1.id,
        body = ServiceConfigurationEntity(
          name = "X",
          label = "Y",
          url = "Z",
          category = PROBATION.name,
          enabled = false,
          templateMigrated = false,
        ),
        roles = listOf(role),
      ).expectStatus()
        .isOk
        .expectBody(ServiceInfo::class.java)
        .returnResult()

      val latest = serviceConfigurationService.getById(s1.id)
      assertThat(latest).isNotNull
      assertThat(latest!!.serviceName).isEqualTo("X")
      assertThat(latest.label).isEqualTo("Y")
      assertThat(latest.url).isEqualTo("Z")
      assertThat(latest.category).isEqualTo(PROBATION)
      assertThat(latest.enabled).isFalse
      assertThat(latest.templateMigrated).isFalse

      // Assert suspended is not modified
      assertServiceConfigurationIsNotSuspended(s1.id)
    }
  }

  @Nested
  inner class SuspendService {

    @Test
    fun `should return status 401 if no auth header is provided`() {
      webTestClient
        .patch()
        .uri("/api/services/${UUID.randomUUID()}/suspend?suspended=true")
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should get status forbidden when token does not have the required roles`() {
      webTestClient
        .patch()
        .uri("/api/services/${UUID.randomUUID()}/suspend?suspended=true")
        .headers(setAuthorisation())
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should return status 500 when suspended parameter is not a valid boolean`(
      role: String,
    ) {
      ensureServiceConfigurationExists(s1)
      assertServiceConfigurationIsNotSuspended(s1.id)

      webTestClient
        .patch()
        .uri("/api/services/${s1.id}/suspend?suspended=xyz")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().is5xxServerError

      assertServiceConfigurationIsNotSuspended(s1.id)
    }

    @Test
    fun `should return status 404 if requested service is not found`() {
      webTestClient
        .patch()
        .uri("/api/services/${UUID.randomUUID()}/suspend?suspended=true")
        .headers(setAuthorisation(roles = createAndUpdateServiceRoles()))
        .exchange()
        .expectStatus().isNotFound
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should update service configuration to suspended true`(role: String) {
      hmppsAuth.stubGrantToken()
      manageUsersApi.stubGetUserDetails("AUTH_ADM")
      ensureServiceConfigurationExists(s1)

      assertServiceConfigurationIsNotSuspended(s1.id)
      val start = Instant.now()

      val response = webTestClient
        .patch()
        .uri("/api/services/${s1.id}/suspend?suspended=true")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().isOk
        .expectBody<Map<String, Any>>()
        .returnResult()

      val actual = assertServiceConfigurationIsSuspended(id = s1.id, suspendedAfter = start)
      assertSuspendedAtFormat(response.responseBody)
      val expectedParameters = mapOf(
        "product" to "A",
        "user" to "John Smith",
        "datetime" to LocalDateTime.ofInstant(actual.suspendedAt, UTC)
          .format(DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm:ss")),
      )
      verify(notificationClient).sendEmail(
        "81c6f1d3-f0c3-40f8-97d1-6cd29c023bfe",
        "suspend@test.com",
        expectedParameters,
        null,
      )
      manageUsersApi.verifyGetUserDetailsApiCalled("AUTH_ADM")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should update service configuration to suspendedAt when suspending an already suspended service`(role: String) {
      val start = Instant.now()
      hmppsAuth.stubGrantToken()
      manageUsersApi.stubGetUserDetails("AUTH_ADM")
      serviceConfigurationService.createServiceConfiguration(s1)
      serviceConfigurationService.updateSuspended(id = s1.id, suspended = true)
      assertServiceConfigurationIsSuspended(id = s1.id, suspendedAfter = start)

      val startUpdate = Instant.now()
      webTestClient
        .patch()
        .uri("/api/services/${s1.id}/suspend?suspended=true")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().isOk
      val actual = assertServiceConfigurationIsSuspended(id = s1.id, suspendedAfter = startUpdate)
      val expectedParameters = mapOf(
        "product" to "A",
        "user" to "John Smith",
        "datetime" to LocalDateTime.ofInstant(actual.suspendedAt, UTC)
          .format(DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm:ss")),
      )
      verify(notificationClient).sendEmail(
        "81c6f1d3-f0c3-40f8-97d1-6cd29c023bfe",
        "suspend@test.com",
        expectedParameters,
        null,
      )
      manageUsersApi.verifyGetUserDetailsApiCalled("AUTH_ADM")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.ServicesControllerIntTest#createAndUpdateServiceRoles")
    fun `should update service configuration to suspended false`(role: String) {
      val start = Instant.now()
      hmppsAuth.stubGrantToken()
      manageUsersApi.stubGetUserDetails("AUTH_ADM")
      serviceConfigurationService.createServiceConfiguration(
        s1.also {
          it.suspended = true
          it.suspendedAt = Instant.now()
        },
      )
      assertServiceConfigurationIsSuspended(id = s1.id, suspendedAfter = start)

      webTestClient
        .patch()
        .uri("/api/services/${s1.id}/suspend?suspended=false")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().isOk

      assertServiceConfigurationIsNotSuspended(id = s1.id)
      val expectedParameters = mapOf(
        "product" to "A",
        "user" to "John Smith",
        "datetime" to "20 March 2026 09:45:23",
      )
      verify(notificationClient).sendEmail(
        "07a8f1fd-2ca9-49a3-b7b7-53ae05b15c11",
        "unsuspend@test.com",
        expectedParameters,
        null,
      )
      manageUsersApi.verifyGetUserDetailsApiCalled("AUTH_ADM")
    }
  }

  private fun createServiceConfiguration(
    body: ServiceConfigurationEntity,
    roles: List<String>,
  ) = webTestClient.post()
    .uri("/api/services")
    .bodyValue(body)
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = roles))
    .exchange()

  private fun putServiceConfiguration(
    id: UUID,
    body: ServiceConfigurationEntity,
    roles: List<String>,
  ) = webTestClient.put()
    .uri("/api/services/$id")
    .bodyValue(body)
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = roles))
    .exchange()

  private fun ensureServiceConfigurationExists(s: ServiceConfiguration) {
    serviceConfigurationService.createServiceConfiguration(s)
    serviceConfigurationService.getByServiceName(s.serviceName)?.let {
      assertThat(it).isNotNull
    } ?: fail { "expected service ${s.serviceName} did not exist" }
  }

  private fun assertServiceConfigurationIsNotSuspended(id: UUID) {
    serviceConfigurationService.getById(id)?.let {
      assertThat(it).isNotNull
      assertThat(it.suspended).isFalse()
      assertThat(it.suspendedAt).isNull()
    } ?: fail { "expected service $id did not exist" }
  }

  private fun assertServiceConfigurationIsSuspended(id: UUID, suspendedAfter: Instant): ServiceConfiguration = serviceConfigurationService
    .getById(id)?.let {
      assertThat(it).isNotNull
      assertThat(it.suspended).isTrue()
      assertThat(it.suspendedAt).isNotNull
      assertThat(it.suspendedAt).isAfter(suspendedAfter)
      assertThat(it.suspendedAt).isBefore(Instant.now())
      it
    } ?: fail { "expected service $id did not exist" }

  private fun assertSuspendedAtFormat(body: Map<String, Any>?) {
    assertThat(body!!.contains("suspendedAt")).isTrue
    assertThat(body["suspendedAt"]).isInstanceOf(String::class.java)
    // expected format: dd/MM/yyyy HH:mm:ss
    assertThat(body["suspendedAt"] as String).matches("""^\d{2}/\d{2}/\d{4} \d{2}:\d{2}:\d{2}$""")
  }

  companion object {
    @JvmStatic
    fun roles(): List<String> = listOf(
      "ROLE_SAR_USER_ACCESS",
      "ROLE_SAR_DATA_ACCESS",
      "ROLE_SAR_SUPPORT",
      "ROLE_SAR_REGISTER_TEMPLATE",
    )

    @JvmStatic
    fun createAndUpdateServiceRoles(): List<String> = listOf(
      "ROLE_SAR_ADMIN_ACCESS",
      "ROLE_SAR_SUPPORT",
    )
  }

  private fun ServiceConfiguration.toEntity() = ServiceConfigurationEntity(
    name = this.serviceName,
    label = this.label,
    url = this.url,
    category = this.category.name,
    enabled = this.enabled,
    templateMigrated = this.templateMigrated,
  )
}
