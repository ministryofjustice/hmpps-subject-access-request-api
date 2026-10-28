package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.RendererServiceFailureType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceErrorNotificationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import java.time.LocalDate

class SubjectAccessRequestControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  @Autowired
  private lateinit var templateVersionRepository: TemplateVersionRepository

  @MockitoBean
  private lateinit var slackNotificationService: SlackNotificationService

  private val createSubjectAccessRequest = CreateSubjectAccessRequestEntity(
    nomisId = "A1111AA",
    ndeliusId = null,
    services = listOf("service1"),
    sarCaseReferenceNumber = "mockedCaseReference",
    dateTo = LocalDate.of(2022, 12, 25),
    dateFrom = LocalDate.of(2001, 1, 1),
  )

  private val serviceConfigOne = ServiceConfiguration(
    serviceName = "service1",
    label = "Service One",
    url = "http://service-one",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val serviceErrorNotification = ServiceErrorNotificationEntity(
    serviceName = "service1",
    failureType = RendererServiceFailureType.SAR_DATA,
    statusCode = 500,
    message = "renderer failed after retries",
  )

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.saveAll(listOf(serviceConfigOne))
  }

  @AfterEach
  fun tearDown() {
    subjectAccessRequestRepository.deleteAll()
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.deleteAllById(listOf(serviceConfigOne.id))
  }

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User without ROLE_SAR_USER_ACCESS can't post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .returnResult()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_SAR_USER_ACCESS", "ROLE_SAR_DATA_ACCESS", "ROLE_SAR_ADMIN_ACCESS"])
  fun `User with eligible role can get subjectAccessRequest by id`(role: String) {
    val saved = subjectAccessRequestRepository.save(
      SubjectAccessRequest(
        sarCaseReferenceNumber = "adminCaseRef",
        nomisId = "A1111AA",
        dateFrom = LocalDate.of(2001, 1, 1),
        dateTo = LocalDate.of(2022, 12, 25),
        requestedBy = "admin",
      ),
    )

    webTestClient.get()
      .uri("/api/subjectAccessRequest/${saved.id}")
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.id").isEqualTo(saved.id.toString())
  }

  @Test
  fun `User with wrong role cannot get subjectAccessRequest by id`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequest/${java.util.UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS cannot post service errors`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequests/${createSubjectAccessRequestEntity().id}/service-errors")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS")))
      .bodyValue(serviceErrorNotification)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can post service errors`() {
    val sar = createSubjectAccessRequestEntity()
    subjectAccessRequestRepository.saveAndFlush(sar)

    webTestClient.post()
      .uri("/api/subjectAccessRequests/${sar.id}/service-errors")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(serviceErrorNotification)
      .exchange()
      .expectStatus()
      .isNoContent

    verify(slackNotificationService).sendRendererServiceCallFailedAlert(any(), any(), any(), any(), any())
  }

  private fun createSubjectAccessRequestEntity() = SubjectAccessRequest(
    nomisId = "A1111AA",
    sarCaseReferenceNumber = "mockedCaseReference",
    dateTo = LocalDate.of(2022, 12, 25),
    dateFrom = LocalDate.of(2001, 1, 1),
    requestedBy = "AUTH_ADM",
  ).also {
    it.services.add(
      RequestServiceDetail(
        subjectAccessRequest = it,
        serviceConfiguration = serviceConfigOne,
        renderStatus = RenderStatus.PENDING,
      ),
    )
  }
}
