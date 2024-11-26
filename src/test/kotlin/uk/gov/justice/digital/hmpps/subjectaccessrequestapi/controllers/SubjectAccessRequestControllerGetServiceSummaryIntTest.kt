package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SubjectAccessRequestControllerGetServiceSummaryIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
  private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()

    val sar1 = SubjectAccessRequest(
      id = UUID.randomUUID(),
      status = Status.Pending,
      dateFrom = dateFrom,
      dateTo = dateTo,
      sarCaseReferenceNumber = "666xzy",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "hansGruber99",
      requestedBy = "Hans Gruber",
      requestDateTime = LocalDateTime.now().minusHours(48),
      claimAttempts = 0,
      claimDateTime = null,
    )

    subjectAccessRequestRepository.save(sar1)
  }

  @AfterEach
  fun tearDown() {
    subjectAccessRequestRepository.deleteAll()
  }

  @Test
  fun `should return status 401 when no auth header present`() {
    webTestClient
      .get()
      .uri("/api/subjectAccessRequests/summary")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return status 403 when required auth role is not present`() {
    webTestClient
      .get()
      .uri("/api/subjectAccessRequests/summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS", "ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isForbidden
  }

  /**
   * See resources/application-test.yml for alarm configuration for Spring "test" profile.
   */
  @Test
  fun `should return expected service summary`() {
    webTestClient
      .get()
      .uri("/api/subjectAccessRequests/summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_SUPPORT")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.backlog.count").isEqualTo(1)
      .jsonPath("$.backlog.alertThreshold").isEqualTo(100)
      .jsonPath("$.backlog.alertFrequency").isEqualTo("720 Minutes")
      .jsonPath("$.overdueReports.count").isEqualTo(1)
      .jsonPath("$.overdueReports.alertThreshold")
      .isEqualTo("status == pending && requestDateTime < (time.now - 12 Hours)")
      .jsonPath("$.overdueReports.alertFrequency").isEqualTo("720 Minutes")
  }
}
