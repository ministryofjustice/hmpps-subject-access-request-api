package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.DuplicateRequestResponseEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class SubjectAccessRequestControllerDuplicateRequestIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  @Autowired
  private lateinit var templateVersionRepository: TemplateVersionRepository

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val nomisId = "NomisId123"
  private val ndeliusId: String? = null
  private val dateFrom = LocalDate.now().minusYears(1)
  private val dateTo = LocalDate.now().minusYears(1)
  private val sarCaseReferenceNumber = "sarCaseReferenceNumber6662"
  private val services = listOf("service1", "service2", "service3")
  private val sarUser = "SAR_USER"
  private val sarAdminUser = "SAR_ADMIN_USER"

  private val serviceConfigOne = ServiceConfiguration(
    UUID.randomUUID(),
    "service1",
    "Service One",
    "http://service-one",
    true,
    true,
    ServiceCategory.PRISON,
  )
  private val serviceConfigTwo = ServiceConfiguration(
    UUID.randomUUID(),
    "service2",
    "Service Two",
    "http://service-two",
    true,
    true,
    ServiceCategory.PRISON,
  )
  private val serviceConfigThree = ServiceConfiguration(
    UUID.randomUUID(),
    "service3",
    "Service Three",
    "http://service-three",
    true,
    true,
    ServiceCategory.PRISON,
  )

  @BeforeEach
  internal fun setup() {
    subjectAccessRequestRepository.deleteAll()
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.saveAll(listOf(serviceConfigOne, serviceConfigTwo, serviceConfigThree))
  }

  @AfterEach
  fun tearDown() {
    subjectAccessRequestRepository.deleteAll()
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.deleteAllById(listOf(serviceConfigOne.id, serviceConfigTwo.id, serviceConfigThree.id))
  }

  @Test
  fun `duplicate should create new subject access request copying the details of the existing request`() {
    val originalRequest = createSubjectAccessRequest()

    val resubmitResponse = resubmitRequest(originalRequest.id)
      .expectStatus().isCreated
      .returnResult(DuplicateRequestResponseEntity::class.java)
      .responseBody
      .blockFirst()

    assertResubmitResponseMatchesExpected(
      resubmitResponse,
      originalRequest.id.toString(),
      originalRequest.sarCaseReferenceNumber,
    )

    val resubmittedRequest = subjectAccessRequestRepository.findById(UUID.fromString(resubmitResponse!!.id)).get()

    assertSubjectAccessRequestMatchesExpected(
      actual = resubmittedRequest,
      expectedId = resubmittedRequest.id,
      expectedNomisId = originalRequest.nomisId,
      expectedNdeliusId = originalRequest.ndeliusCaseReferenceId,
      expectedDateFrom = originalRequest.dateFrom,
      expectedDateTo = originalRequest.dateTo,
      expectedSarCaseReferenceNumber = originalRequest.sarCaseReferenceNumber,
      expectedServices = originalRequest.services.map { it.serviceConfiguration.serviceName },
      expectedRequestedBy = sarAdminUser,
    )
  }

  @Test
  fun `should return 404 status if original request ID does not exist`() {
    val originalRequestId = UUID.randomUUID()

    resubmitRequest(originalRequestId)
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo(404)
      .jsonPath("$.errorCode").isEqualTo(null)
      .jsonPath("$.userMessage").isEqualTo("duplicate subject access request unsuccessful: request ID not found")
      .jsonPath("$.developerMessage").isEqualTo("duplicate subject access request unsuccessful: request ID not found")
      .jsonPath("$.moreInfo").isEqualTo("SubjectAccessRequestId: $originalRequestId")
  }

  fun createSubjectAccessRequest(): SubjectAccessRequest {
    val id = postSubjectAccessRequest()
    assertThat(id).isNotNull
    assertThat(id).isNotEmpty()

    val request = subjectAccessRequestRepository.findById(UUID.fromString(id))

    assertThat(request).isPresent
    assertSubjectAccessRequestMatchesExpected(
      actual = request.get(),
      expectedId = UUID.fromString(id),
      expectedNomisId = nomisId,
      expectedNdeliusId = ndeliusId,
      expectedDateFrom = dateFrom,
      expectedDateTo = dateTo,
      expectedSarCaseReferenceNumber = sarCaseReferenceNumber,
      expectedServices = services,
      expectedRequestedBy = sarUser,
    )
    return request.get()
  }

  fun assertResubmitResponseMatchesExpected(
    resubmitResponse: DuplicateRequestResponseEntity?,
    expectedId: String,
    expectedSarCaseReference: String,
  ) {
    assertThat(resubmitResponse).isNotNull
    assertThat(resubmitResponse!!.id).isNotNull
    assertThat(resubmitResponse.id).isNotEmpty()
    assertThat(resubmitResponse.originalId).isEqualTo(expectedId)
    assertThat(resubmitResponse.sarCaseReferenceNumber).isEqualTo(expectedSarCaseReference)
  }

  fun assertSubjectAccessRequestMatchesExpected(
    actual: SubjectAccessRequest?,
    expectedId: UUID?,
    expectedNomisId: String?,
    expectedNdeliusId: String?,
    expectedDateFrom: LocalDate?,
    expectedDateTo: LocalDate?,
    expectedSarCaseReferenceNumber: String?,
    expectedServices: List<String>,
    expectedRequestedBy: String?,
  ) {
    assertThat(actual).isNotNull
    assertThat(actual!!.id).isEqualTo(expectedId)
    assertThat(actual.nomisId).isEqualTo(expectedNomisId)
    assertThat(actual.ndeliusCaseReferenceId).isEqualTo(expectedNdeliusId)
    assertThat(actual.dateFrom).isEqualTo(expectedDateFrom)
    assertThat(actual.dateTo).isEqualTo(expectedDateTo)
    assertThat(actual.sarCaseReferenceNumber).isEqualTo(expectedSarCaseReferenceNumber)
    assertThat(actual.services).extracting<String> { it.serviceConfiguration.serviceName }.containsExactlyElementsOf(expectedServices)
    assertThat(actual.requestedBy).isEqualTo(expectedRequestedBy)
  }

  fun resubmitRequest(id: UUID) = webTestClient
    .post()
    .uri("/api/subjectAccessRequests/$id/duplicate")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = sarAdminUser, roles = listOf("ROLE_SAR_SUPPORT")))
    .exchange()

  fun postSubjectAccessRequest(): String {
    val body = webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = sarUser, roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(
        """
          {
            "nomisId": "$nomisId",
            "ndeliusId": null,
            "dateFrom": "${dateFormatter.format(dateFrom)}",
            "dateTo": "${dateFormatter.format(dateTo)}",
            "sarCaseReferenceNumber": "$sarCaseReferenceNumber",
            "services": ${services.map {"\"$it\""}}
          }
        """.trimIndent(),
      ).exchange()
      .expectStatus().isCreated
      .expectBody()
      .returnResult().responseBody

    assertThat(body).isNotNull()
    return String(body!!)
  }
}
