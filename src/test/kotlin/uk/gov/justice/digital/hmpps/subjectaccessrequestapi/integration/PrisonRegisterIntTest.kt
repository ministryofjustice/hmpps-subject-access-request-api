package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.PrisonDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.PrisonRegisterApiExtension.Companion.prisonRegisterApi

@ActiveProfiles("test")
class PrisonRegisterIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRegisterClient: PrisonRegisterClient

  @Test
  fun `Prison register returns prison names`() {
    prisonRegisterApi.stubGetPrisonDetails()

    val response = prisonRegisterClient.getPrisonDetails()
    val prisonDetailsList = listOf(
      PrisonDetails(prisonId = "AKI", prisonName = "Acklington (HMP)"),
      PrisonDetails(prisonId = "ALI", prisonName = "Albany (HMP)"),
      PrisonDetails(prisonId = "ANI", prisonName = "Aldington (HMP)"),
    )

    assertThat(response).isNotNull
    assertThat(response[0].prisonId).isEqualTo("AKI")
    assertThat(response[0].prisonName).isEqualTo("Acklington (HMP)")
    assertThat(response[1].prisonId).isEqualTo("ALI")
    assertThat(response[1].prisonName).isEqualTo("Albany (HMP)")
    assertThat(response[2].prisonId).isEqualTo("ANI")
    assertThat(response[2].prisonName).isEqualTo("Aldington (HMP)")
  }
}
