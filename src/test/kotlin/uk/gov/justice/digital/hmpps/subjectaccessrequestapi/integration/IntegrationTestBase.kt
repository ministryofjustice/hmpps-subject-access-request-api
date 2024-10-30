package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension.Companion.documentServiceApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisUserRolesApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisUserRolesApiExtension.Companion.nomisUserRolesApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.PrisonRegisterApiExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.PrisonRegisterApiExtension.Companion.prisonRegisterApi
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, DocumentServiceApiExtension::class, PrisonRegisterApiExtension::class, NomisUserRolesApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
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
    nomisUserRolesApi.stubHealthPing(status)
  }
}