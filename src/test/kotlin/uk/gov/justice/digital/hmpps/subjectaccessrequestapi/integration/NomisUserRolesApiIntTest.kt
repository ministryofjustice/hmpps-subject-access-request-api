package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.UserDetailsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisUserRolesApiExtension.Companion.nomisUserRolesApi

@ActiveProfiles("test")
class NomisUserRolesApiIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var userDetailsClient: UserDetailsClient

  @Test
  fun `Nomis User Roles returns user last names`() {
    hmppsAuth.stubGrantToken()
    nomisUserRolesApi.stubGetUserDetails()

    val response = userDetailsClient.getNomisUserDetails()

    assertThat(response).isNotNull
    assertThat(response[0].username).isEqualTo("AA46243")
    assertThat(response[0].lastName).isEqualTo("SMITH")
    assertThat(response[1].username).isEqualTo("ALI241")
    assertThat(response[1].lastName).isEqualTo("JONES")
    assertThat(response[2].username).isEqualTo("DB128Z")
    assertThat(response[2].lastName).isEqualTo("ALI")
  }
}
