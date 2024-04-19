package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.JwtAuthHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisation(
    user: String = "subject-access-request-1",
    roles: List<String> = listOf(),
    name: String = "hmpps-subject-access-request-1",
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, name)
}
