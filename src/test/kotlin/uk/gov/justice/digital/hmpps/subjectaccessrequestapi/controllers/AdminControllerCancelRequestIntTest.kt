package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AdminControllerCancelRequestIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  companion object {
    private val REQUEST_ID = UUID.fromString("8caf5cb9-0f25-4b6e-b400-f0c95ac3fc97")
    private val NOW = LocalDateTime.of(2026, 1, 1, 12, 1)

    @JvmStatic
    private fun invalidCancelStatuses(): List<Status> = listOf(
      Status.Errored,
      Status.Cancelled,
      Status.Completed,
    )
  }

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()
  }

  private fun createSubjectAccessRequestWithStatus(status: Status): SubjectAccessRequest = SubjectAccessRequest(
    id = REQUEST_ID,
    status = status,
    dateFrom = LocalDate.parse("2020-01-01"),
    dateTo = LocalDate.parse("2025-01-01"),
    sarCaseReferenceNumber = "666xzy",
    nomisId = "",
    ndeliusCaseReferenceId = "hansGruber99",
    requestedBy = "Hans Gruber",
    requestDateTime = NOW.minusHours(48),
    claimAttempts = 0,
    claimDateTime = null,
  )

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.put()
      .uri("/api/admin/subjectAccessRequests/$REQUEST_ID/cancel")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.put()
      .uri("/api/admin/subjectAccessRequests/$REQUEST_ID/cancel")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return status NOT FOUND when cancel request is given an unknown ID`() {
    webTestClient.put()
      .uri("/api/admin/subjectAccessRequests/$REQUEST_ID/cancel")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_ADMIN_ACCESS")))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo("404")
      .jsonPath("$.userMessage").isEqualTo("failed to cancel subject access request, request not found")
      .jsonPath("$.moreInfo").isEqualTo("SubjectAccessRequestId: $REQUEST_ID")
  }

  @ParameterizedTest(name = "status={0}")
  @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.AdminControllerCancelRequestIntTest#invalidCancelStatuses")
  fun `should return status Bad Request when cancel request attempted on a request with status `(status: Status) {
    val req = subjectAccessRequestRepository.save(createSubjectAccessRequestWithStatus(status))
    webTestClient.put()
      .uri("/api/admin/subjectAccessRequests/${req.id}/cancel")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_ADMIN_ACCESS")))
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody()
      .jsonPath("$.status").isEqualTo("409")
      .jsonPath("$.userMessage").isEqualTo("only subject access requests with status 'Pending' can be cancelled, actual status: '${status.name}'")
      .jsonPath("$.moreInfo").isEqualTo("SubjectAccessRequestId: $REQUEST_ID")
  }
}
