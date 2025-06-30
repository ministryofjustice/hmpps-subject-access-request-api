package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension.Companion.documentServiceApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceAltHealthExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceAltHealthExtension.Companion.dynamicServiceAlt
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DynamicServiceExtension.Companion.dynamicService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.ExternalUserApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.ExternalUserApiExtension.Companion.externalUserApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.LocationsApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.LocationsApiExtension.Companion.locationsApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisMappingsApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisMappingsApiExtension.Companion.nomisMappingsApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisUserRolesApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisUserRolesApiExtension.Companion.nomisUserRolesApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.PrisonRegisterApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.PrisonRegisterApiExtension.Companion.prisonRegisterApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.SarAndDeliusApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.SarAndDeliusApiExtension.Companion.sarAndDeliusApi
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(
  HmppsAuthApiExtension::class,
  DocumentServiceApiExtension::class,
  PrisonRegisterApiExtension::class,
  ExternalUserApiExtension::class,
  NomisUserRolesApiExtension::class,
  SarAndDeliusApiExtension::class,
  LocationsApiExtension::class,
  NomisMappingsApiExtension::class,
  DynamicServiceExtension::class,
  DynamicServiceAltHealthExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "12000")
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    documentServiceApi.stubHealthPing(status)
    prisonRegisterApi.stubHealthPing(status)
    externalUserApi.stubHealthPing(status)
    nomisUserRolesApi.stubHealthPing(status)
    sarAndDeliusApi.stubHealthPing(status)
    locationsApi.stubHealthPing(status)
    nomisMappingsApi.stubHealthPing(status)
    dynamicService.stubHealthPing(status)
    dynamicServiceAlt.stubAltHealth(status)
  }
}
