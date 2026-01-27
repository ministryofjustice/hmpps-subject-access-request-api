package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.health.contributor.Status
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DYNAMIC_SERVICE_ALT_PORT
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DYNAMIC_SERVICE_PORT
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceAltHealthExtension.Companion.dynamicServiceAlt
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceExtension.Companion.dynamicService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import java.util.UUID

@ActiveProfiles("test")
class DynamicServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var dynamicServicesClient: DynamicServicesClient

  private val serviceConfiguration = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test1",
    label = "Test One",
    url = "http://localhost:${DYNAMIC_SERVICE_PORT}",
    order = 123,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  @Test
  fun `Can successfully get health ping for dynamic service when up`() {
    dynamicService.stubHealthPing(200)

    val response = dynamicServicesClient.getServiceHealthPing("http://localhost:${DYNAMIC_SERVICE_PORT}")

    assertThat(response).isNotNull
    assertThat(response.status).isEqualTo(Status.UP)
  }

  @ParameterizedTest
  @ValueSource(ints = [ 404, 500, 502 ])
  fun `Can successfully get health ping for dynamic service when down`(status: Int) {
    dynamicService.stubHealthPing(status)

    val response = dynamicServicesClient.getServiceHealthPing("http://localhost:${DYNAMIC_SERVICE_PORT}")

    assertThat(response).isNotNull
    assertThat(response.status).isEqualTo(Status.DOWN)
  }

  @Test
  fun `Can successfully get alternative health for dynamic service when up`() {
    dynamicServiceAlt.stubAltHealth(200)

    val response = dynamicServicesClient.getAlternativeServiceHealth("http://localhost:${DYNAMIC_SERVICE_ALT_PORT}")

    assertThat(response).isNotNull
    assertThat(response.status).isEqualTo(Status.UP)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      """200, {"healthy":false}""",
      """200, {"healthy":null}""",
      """200, {}""",
      """200, """,
      """404, {"healthy":false}""",
      """500, {"healthy":false}""",
      """502, {"healthy":false}""",
    ],
  )
  fun `Can successfully get alternative health for dynamic service when down`(status: Int, body: String?) {
    dynamicServiceAlt.stubAltHealth(status, body)

    val response = dynamicServicesClient.getAlternativeServiceHealth("http://localhost:${DYNAMIC_SERVICE_ALT_PORT}")

    assertThat(response).isNotNull
    assertThat(response.status).isEqualTo(Status.DOWN)
  }

  @Test
  fun `Can get template for service`() {
    dynamicService.stubGetTemplate(200)

    val response = dynamicServicesClient.getServiceTemplate(serviceConfiguration)

    assertThat(response).isNotNull.isEqualTo("<h1>Template one</h2>")
  }

  @ParameterizedTest
  @ValueSource(
    ints = [ 400, 401, 403, 404, 500, 502 ],
  )
  fun `Returns null when get template for service and error status`(status: Int) {
    dynamicService.stubGetTemplate(status)

    val response = dynamicServicesClient.getServiceTemplate(serviceConfiguration)

    assertThat(response).isNull()
  }
}
