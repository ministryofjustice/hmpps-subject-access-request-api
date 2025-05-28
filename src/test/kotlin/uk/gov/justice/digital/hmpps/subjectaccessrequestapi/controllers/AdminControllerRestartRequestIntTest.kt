package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class AdminControllerRestartRequestIntTest : IntegrationTestBase() {

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
      status = Status.Errored,
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
    webTestClient.patch()
      .uri("/api/admin/subjectAccessRequests/${REQUEST_ID}/restart")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.patch()
      .uri("/api/admin/subjectAccessRequests/${REQUEST_ID}/restart")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_ADMIN_ACCESS can restart subjectAccessRequest`() {
    webTestClient.patch()
      .uri("/api/admin/subjectAccessRequests/${REQUEST_ID}/restart")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_ADMIN_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk

    assertThat(subjectAccessRequestRepository.findById(REQUEST_ID))
      .hasValueSatisfying { it: SubjectAccessRequest ->
        assertThat(it.status).isEqualTo(Status.Pending)
        assertThat(it.requestDateTime).isCloseTo(LocalDateTime.now(), within(5, SECONDS))
      }
  }
}
