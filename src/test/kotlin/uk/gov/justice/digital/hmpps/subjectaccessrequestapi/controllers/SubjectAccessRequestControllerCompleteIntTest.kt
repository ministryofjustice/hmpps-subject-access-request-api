package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension.Companion.documentServiceApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SubjectAccessRequestControllerCompleteIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var oAuth2AuthorizedClientService: OAuth2AuthorizedClientService

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
  private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()

    // Remove the cache client token to force each test to obtain an Auth token before calling the documentStore API.
    oAuth2AuthorizedClientService.removeAuthorizedClient("sar-client", "AUTH_ADM")
  }

  @Test
  fun `should return status 404 if subject access request id is not found`() {
    val id = UUID.randomUUID()

    webTestClient.patch()
      .uri("/api/subjectAccessRequests/$id/complete")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("complete subject access request unsuccessful request ID not found")
      .jsonPath("status").isEqualTo("404")
      .jsonPath("moreInfo").isEqualTo("SubjectAccessRequestId: $id")
  }

  @Test
  fun `should complete subject access request successfully`() {
    val sar = saveSubjectAccessRequest(Status.Pending)

    webTestClient.patch()
      .uri("/api/subjectAccessRequests/${sar.id}/complete")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk

    val result = subjectAccessRequestRepository.findById(sar.id)
    assertThat(result.isPresent).isTrue()
    assertThat(result.get().status).isEqualTo(Status.Completed)
  }

  @Test
  fun `should not update already completed subject access request`() {
    val sar = saveSubjectAccessRequest(Status.Completed)

    webTestClient.patch()
      .uri("/api/subjectAccessRequests/${sar.id}/complete")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isBadRequest

    val result = subjectAccessRequestRepository.findById(sar.id)
    assertThat(result.isPresent).isTrue()
    assertThat(result.get().status).isEqualTo(Status.Completed)

    verify(telemetryClient, times(1)).trackApiEvent("DuplicateCompleteRequest", sar.id.toString())
  }

  @Nested
  inner class CompleteSubjectAccessRequestWithStatusError {
    @Test
    fun `should delete document from document store and return status 400 if subject access request has an existing status 'Errored'`() {
      val sar = saveSubjectAccessRequest(Status.Errored)

      hmppsAuth.stubGrantToken()
      documentServiceApi.deleteDocumentSuccess(sar.id)

      val expected = subjectAccessRequestRepository.findById(sar.id)
      assertThat(expected.isPresent).isTrue()
      assertThat(expected.get()).isEqualTo(sar)

      webTestClient.patch()
        .uri("/api/subjectAccessRequests/${sar.id}/complete")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("complete request unsuccessful existing status is 'Errored'")
        .jsonPath("status").isEqualTo("400")
        .jsonPath("moreInfo").isEqualTo("SubjectAccessRequestId: ${sar.id}")

      hmppsAuth.verify(1, postRequestedFor(urlPathEqualTo("/auth/oauth/token")))
      documentServiceApi.verify(1, deleteRequestedFor(urlPathEqualTo("/documents/${sar.id}")))
    }

    @Test
    fun `should return error if unable to get auth token before calling the document API`() {
      hmppsAuth.stubGrantTokenError()

      val sar = saveSubjectAccessRequest(Status.Errored)

      webTestClient.patch()
        .uri("/api/subjectAccessRequests/${sar.id}/complete")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().is5xxServerError

      hmppsAuth.verify(1, postRequestedFor(urlPathEqualTo("/auth/oauth/token")))
      documentServiceApi.verify(0, deleteRequestedFor(urlPathEqualTo("/documents/${sar.id}")))
    }

    @Test
    fun `should return bad request if existing status is 'Errored' and delete document request is unsuccessful`() {
      val sar = saveSubjectAccessRequest(Status.Errored)

      hmppsAuth.stubGrantToken()
      documentServiceApi.deleteDocumentError(sar.id, 500)

      webTestClient.patch()
        .uri("/api/subjectAccessRequests/${sar.id}/complete")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("complete request unsuccessful existing status is 'Errored'")
        .jsonPath("status").isEqualTo("400")
        .jsonPath("moreInfo").isEqualTo("SubjectAccessRequestId: ${sar.id}")

      hmppsAuth.verify(1, postRequestedFor(urlPathEqualTo("/auth/oauth/token")))
      documentServiceApi.verify(1, deleteRequestedFor(urlPathEqualTo("/documents/${sar.id}")))
    }
  }

  fun saveSubjectAccessRequest(status: Status): SubjectAccessRequest =
    subjectAccessRequestRepository.save(createSubjectAccessRequest(status))

  fun createSubjectAccessRequest(status: Status) = SubjectAccessRequest(
    id = UUID.randomUUID(),
    status = status,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "666xzy",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "hansGruber99",
    requestedBy = "Hans Gruber",
    requestDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
    claimAttempts = 0,
    claimDateTime = null,
  )
}
