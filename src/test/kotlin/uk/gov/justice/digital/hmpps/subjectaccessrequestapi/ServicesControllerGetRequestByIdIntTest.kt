package uk.gov.justice.digital.hmpps.subjectaccessrequestapi

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

class ServicesControllerGetRequestByIdIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
  private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)

  private val sar = SubjectAccessRequest(
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

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()
    subjectAccessRequestRepository.save(sar)
  }

  @AfterEach
  fun tearDown() {
    subjectAccessRequestRepository.deleteAll()
  }

  @Test
  fun `get report by ID returns 200 when the requested ID exists`() {
    webTestClient
      .get()
      .uri("/api/subjectAccessRequest/${sar.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS", "ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath(".id").isEqualTo(sar.id.toString())
      .jsonPath(".status").isEqualTo("Pending")
      .jsonPath(".dateFrom").isEqualTo(dateFrom.toString())
      .jsonPath(".dateTo").isEqualTo(dateTo.toString())
      .jsonPath(".sarCaseReferenceNumber").isEqualTo("666xzy")
      .jsonPath(".services").isEqualTo("{1,2,4}")
      .jsonPath(".nomisId").isEqualTo("")
      .jsonPath(".ndeliusCaseReferenceId").isEqualTo("hansGruber99")
      .jsonPath(".requestedBy").isEqualTo("Hans Gruber")
  }

  @Test
  fun `get report by ID returns 404 when the requested ID does not exist`() {
    webTestClient
      .get()
      .uri("/api/subjectAccessRequest/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS", "ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNotFound
  }
}
