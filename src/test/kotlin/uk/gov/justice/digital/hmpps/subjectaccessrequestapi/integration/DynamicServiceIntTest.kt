package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Status
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DYNAMIC_SERVICE_ALT_PORT
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DYNAMIC_SERVICE_PORT
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceAltHealthExtension.Companion.dynamicServiceAlt
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceExtension.Companion.dynamicService

@ActiveProfiles("test")
class DynamicServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var dynamicServicesClient: DynamicServicesClient

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
}
