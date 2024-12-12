package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.DuplicateRequestResponseEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class SubjectAccessRequestControllerDuplicateRequestIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val nomisId = "NomisId123"
  private val ndeliusId: String? = null
  private val dateFrom = LocalDate.now().minusYears(1)
  private val dateTo = LocalDate.now().minusYears(1)
  private val sarCaseReferenceNumber = "sarCaseReferenceNumber6662"
  private val services = "[service1,service2,service3]"
  private val sarUser = "SAR_USER"
  private val sarAdminUser = "SAR_ADMIN_USER"

  @BeforeEach
  internal fun setup() {
    subjectAccessRequestRepository.deleteAll()
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

    val resubmittedRequest = subjectAccessRequestRepository.findById(UUID.fromString(resubmitResponse!!.id))

    assertSubjectAccessRequestMatchesExpected(
      actual = resubmittedRequest.get(),
      expectedId = originalRequest.id,
      expectedNomisId = originalRequest.nomisId,
      expectedNdeliusId = originalRequest.ndeliusCaseReferenceId,
      expectedDateFrom = originalRequest.dateFrom,
      expectedDateTo = originalRequest.dateTo,
      expectedSarCaseReferenceNumber = originalRequest.sarCaseReferenceNumber,
      expectedServices = originalRequest.services,
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
    expectedServices: String?,
    expectedRequestedBy: String?,
  ) {
    assertThat(actual).isNotNull
    assertThat(actual!!.nomisId).isEqualTo(expectedNomisId)
    assertThat(actual.ndeliusCaseReferenceId).isEqualTo(expectedNdeliusId)
    assertThat(actual.dateFrom).isEqualTo(expectedDateFrom)
    assertThat(actual.dateTo).isEqualTo(expectedDateTo)
    assertThat(actual.sarCaseReferenceNumber).isEqualTo(expectedSarCaseReferenceNumber)
    assertThat(actual.services).isEqualTo(expectedServices)
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
            "services": "$services"
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
