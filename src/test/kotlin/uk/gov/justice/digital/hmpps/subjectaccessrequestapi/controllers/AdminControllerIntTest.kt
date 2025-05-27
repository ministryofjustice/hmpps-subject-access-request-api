package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ExtendedSubjectAccessRequestDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequestAdminSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AdminControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  companion object {
    private val REQUEST_ID = UUID.fromString("8caf5cb9-0f25-4b6e-b400-f0c95ac3fc97")
  }

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()

    val sar1 = SubjectAccessRequest(
      id = REQUEST_ID,
      status = Status.Pending,
      dateFrom = LocalDate.parse("2020-01-01"),
      dateTo = LocalDate.parse("2025-01-01"),
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
  fun `should return unauthorized if no token`() {
    webTestClient.get()
      .uri("/api/admin/subjectAccessRequests")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/api/admin/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_ADMIN_ACCESS can get subjectAccessRequest admin summary`() {
    val result = webTestClient.get()
      .uri("/api/admin/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_ADMIN_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SubjectAccessRequestAdminSummary::class.java)
      .responseBody
      .blockFirst()

    assertThat(result).usingRecursiveComparison().ignoringFields("requests.requestDateTime").isEqualTo(
      SubjectAccessRequestAdminSummary(
        totalCount = 1,
        completedCount = 0,
        pendingCount = 0,
        overdueCount = 1,
        erroredCount = 0,
        filterCount = 1,
        requests = listOf(
          ExtendedSubjectAccessRequestDetail(
            id = REQUEST_ID,
            status = "Overdue",
            dateFrom = LocalDate.parse("2020-01-01"),
            dateTo = LocalDate.parse("2025-01-01"),
            sarCaseReferenceNumber = "666xzy",
            services = "{1,2,4}",
            nomisId = "",
            ndeliusCaseReferenceId = "hansGruber99",
            requestedBy = "Hans Gruber",
            requestDateTime = LocalDateTime.now().minusHours(48),
            claimAttempts = 0,
            claimDateTime = null,
            objectUrl = null,
            lastDownloaded = null,
            appInsightsEventsUrl = "https://portal.azure.com/#blade/Microsoft_Azure_Monitoring_Logs/LogsBlade/resourceId/" +
              "%2Fsubscriptions%2F11111111-1111-1111-1111-111111111111" +
              "%2FresourceGroups%2Fnomisapi-t3-rg" +
              "%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-t3" +
              "/source/LogsBlade.AnalyticsShareLinkToQuery" +
              "/q/H4sIAAAAAAAA%2F5XMrQ7CMBAA4Fc5VxBNGtiAibkhMAiSKUJIe7tmhf6MXQuGhyfBovge4MPCOYX9k2JmeMNrpJkAfSrD9ZQ8HXUg8O5OIMYwTSy5mBthlhqRmOVMj0KcBeg4wAK%2FWecCRXYp8ln0%2FaETF2hbEDvUtkbTSGVXtazMhqSplJJWYVNrXFtstgLSDL%2BLG%2F45lh8cxBkY1AAAAA%3D%3D" +
              "/timespan/PT168H",
          ),
        ),
      ),
    )
  }
}
