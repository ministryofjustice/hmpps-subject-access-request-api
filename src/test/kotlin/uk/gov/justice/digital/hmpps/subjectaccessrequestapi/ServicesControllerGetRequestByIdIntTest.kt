package uk.gov.justice.digital.hmpps.subjectaccessrequestapi

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ServicesControllerGetRequestByIdIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
  private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)

  val serviceConfigurationOne = ServiceConfiguration(
    serviceName = "service-one",
    label = "Service One",
    url = "http://service-one",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )
  val serviceConfigurationTwo = ServiceConfiguration(
    serviceName = "service-two",
    label = "Service Two",
    url = "http://service-two",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )
  private val sar = SubjectAccessRequest(
    id = UUID.randomUUID(),
    status = Status.Pending,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "666xzy",
    nomisId = "",
    ndeliusCaseReferenceId = "hansGruber99",
    requestedBy = "Hans Gruber",
    requestDateTime = LocalDateTime.now().minusHours(48),
    claimAttempts = 0,
    claimDateTime = null,
  ).also {
    it.services.add(
      RequestServiceDetail(
        subjectAccessRequest = it,
        serviceConfiguration = serviceConfigurationOne,
        renderStatus = RenderStatus.PENDING,
        templateVersion = "2",
        renderedAt = LocalDateTime.parse("2026-02-28T12:14:34"),
      ),
    )
    it.services.add(
      RequestServiceDetail(
        subjectAccessRequest = it,
        serviceConfiguration = serviceConfigurationTwo,
        renderStatus = RenderStatus.COMPLETE,
        templateVersion = "6",
        renderedAt = LocalDateTime.parse("2026-03-01T09:46:04"),
      ),
    )
  }

  @BeforeEach
  fun setup() {
    serviceConfigurationRepository.deleteAll()
    serviceConfigurationRepository.saveAll(listOf(serviceConfigurationOne, serviceConfigurationTwo))
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
      .jsonPath(".services").exists()
      .jsonPath(".services[0]").exists()
      .jsonPath(".services[0].serviceName").isEqualTo("service-one")
      .jsonPath(".services[0].renderStatus").isEqualTo("PENDING")
      .jsonPath(".services[0].templateVersion").isEqualTo("2")
      .jsonPath(".services[0].renderedAt").isEqualTo("2026-02-28T12:14:34")
      .jsonPath(".services[1]").exists()
      .jsonPath(".services[1].serviceName").isEqualTo("service-two")
      .jsonPath(".services[1].renderStatus").isEqualTo("COMPLETE")
      .jsonPath(".services[1].templateVersion").isEqualTo("6")
      .jsonPath(".services[1].renderedAt").isEqualTo("2026-03-01T09:46:04")
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
