package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.*

class SubjectAccessRequestControllerTest {
  private val requestTime = LocalDateTime.now()
  private val sarService = Mockito.mock(SubjectAccessRequestService::class.java)
  private val auditService = Mockito.mock(AuditService::class.java)
  private val authentication: Authentication = Mockito.mock(Authentication::class.java)
  private val telemetryClient = Mockito.mock(TelemetryClient::class.java)
  private val ndeliusRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  @Test
  fun `createSubjectAccessRequest post calls service createSubjectAccessRequest with same parameters`() {
    Mockito.`when`(authentication.name).thenReturn("mockUserName")
    Mockito.`when`(sarService.createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)).thenReturn("")
    val expected: ResponseEntity<String> = ResponseEntity("", HttpStatus.OK)

    val result = SubjectAccessRequestController(sarService, auditService, telemetryClient)
      .createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)

    verify(sarService, times(1)).createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequest post returns http error if both nomis and ndelius ids are provided`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val ndeliusAndNomisRequest = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: null, " +
      "ndeliusId: '1' " +
      "}"
    Mockito.`when`(sarService.createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime))
      .thenReturn("Both nomisId and ndeliusId are provided - exactly one is required")

    val response = SubjectAccessRequestController(sarService, auditService, telemetryClient)
      .createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)

    verify(sarService, times(1)).createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)
    val expected: ResponseEntity<String> =
      ResponseEntity("Both nomisId and ndeliusId are provided - exactly one is required", HttpStatus.BAD_REQUEST)
    Assertions.assertThat(response).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequest post returns http error if neither nomis nor ndelius ids are provided`() {
    val noIDRequest = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: null, " +
      "ndeliusId: null " +
      "}"

    Mockito.`when`(sarService.createSubjectAccessRequest(noIDRequest, authentication, requestTime))
      .thenReturn("Neither nomisId nor ndeliusId is provided - exactly one is required")
    Mockito.`when`(authentication.name).thenReturn("mockUserName")

    val response = SubjectAccessRequestController(sarService, auditService, telemetryClient)
      .createSubjectAccessRequest(noIDRequest, authentication, requestTime)
    verify(sarService, times(1)).createSubjectAccessRequest(noIDRequest, authentication, requestTime)
    val expected: ResponseEntity<String> =
      ResponseEntity("Neither nomisId nor ndeliusId is provided - exactly one is required", HttpStatus.BAD_REQUEST)
    Assertions.assertThat(response).isEqualTo(expected)
  }

  @Nested
  inner class GetSubjectAccessRequests {
    @Test
    fun `getSubjectAccessRequests is called with unclaimedOnly, search and pagination parameters if specified in controller and returns list`() {
      val result: List<SubjectAccessRequest?> =
        SubjectAccessRequestController(sarService, auditService, telemetryClient)
          .getSubjectAccessRequests(unclaimed = true, search = "testSearchString", pageNumber = 1, pageSize = 1)

      verify(sarService, times(1)).getSubjectAccessRequests(unclaimedOnly = true, search = "testSearchString", pageNumber = 1, pageSize = 1)
      Assertions.assertThatList(result)
    }

    @Test
    fun `getSubjectAccessRequests is called with unclaimedOnly = false, search = '' and no pagination parameters if unspecified in controller`() {
      SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .getSubjectAccessRequests()

      verify(sarService, times(1)).getSubjectAccessRequests(unclaimedOnly = false, search = "", pageNumber = null, pageSize = null)
    }
  }

  @Nested
  inner class GetTotalSubjectAccessRequests {
    @Test
    fun `getTotalSubjectAccessRequests calls getSubjectAccessRequests with unclaimedOnly = false `() {
      SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .getTotalSubjectAccessRequests()
      verify(sarService, times(1)).getSubjectAccessRequests(unclaimedOnly = false, search = "")
    }
  }

  @Nested
  inner class PatchSubjectAccessRequest {
    private val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")

    @Test
    fun `claimSubjectAccessRequest returns Bad Request if updateSubjectAccessRequest returns 0 with claim time update`() {
      Mockito.`when`(sarService.claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))).thenReturn(0)

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .claimSubjectAccessRequest(testUuid)

      verify(sarService, times(1)).claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))
      Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `claimSubjectAccessRequest returns Response OK if updateSubjectAccessRequest returns 1 with time update`() {
      Mockito.`when`(sarService.claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))).thenReturn(1)

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .claimSubjectAccessRequest(testUuid)

      verify(sarService, times(1)).claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))
      Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `completeSubjectAccessRequest returns Bad Request if completeSubjectAccessRequest returns 0 with status update`() {
      Mockito.`when`(sarService.completeSubjectAccessRequest(testUuid)).thenReturn(0)

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .completeSubjectAccessRequest(testUuid)

      verify(sarService, times(1)).completeSubjectAccessRequest(testUuid)
      Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `completeSubjectAccessRequest returns Response OK if completeSubjectAccessRequest returns 1 with status update`() {
      Mockito.`when`(sarService.completeSubjectAccessRequest(testUuid)).thenReturn(1)

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient)
        .completeSubjectAccessRequest(testUuid)

      verify(sarService, times(1)).completeSubjectAccessRequest(testUuid)
      Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    }
  }

  @Nested
  inner class GetReport {
    private val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")

    @Test
    fun `getReport returns 200 if service retrieveSubjectAccessRequestDocument returns a response`() {
      val mockByteArrayInputStream = Mockito.mock(ByteArrayInputStream::class.java)
      val mockStream = Flux.just(InputStreamResource(mockByteArrayInputStream))
      Mockito.`when`(sarService.retrieveSubjectAccessRequestDocument(testUuid))
        .thenReturn(ResponseEntity.ok().contentType(MediaType.parseMediaType("application/pdf")).body(mockStream))

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient).getReport(testUuid)
      verify(sarService, times(1)).retrieveSubjectAccessRequestDocument(testUuid)
      Assertions.assertThat(result).isEqualTo(
        ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("application/pdf"))
          .body(InputStreamResource(mockByteArrayInputStream)),
      )
    }

    @Test
    fun `getReport returns 404 if service retrieveSubjectAccessRequestDocument does not return a response`() {
      val errorMessage = "Report Not Found"
      Mockito.`when`(sarService.retrieveSubjectAccessRequestDocument(testUuid)).thenReturn(null)

      val result = SubjectAccessRequestController(sarService, auditService, telemetryClient).getReport(testUuid)

      verify(sarService, times(1)).retrieveSubjectAccessRequestDocument(testUuid)
      Assertions.assertThat(result).isEqualTo(
        ResponseEntity(errorMessage, HttpStatus.NOT_FOUND),
      )
    }
  }

  @Nested
  inner class EndpointResponses : IntegrationTestBase() {
    @Test
    fun `User without ROLE_SAR_USER_ACCESS can't post subjectAccessRequest`() {
      webTestClient.post()
        .uri("/api/subjectAccessRequest")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS_DENIED")))
        .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
    }

    @Test
    fun `User with ROLE_SAR_USER_ACCESS can post subjectAccessRequest`() {
      webTestClient.post()
        .uri("/api/subjectAccessRequest")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS"), name = "INTEGRATION_TEST_USER"))
        .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
    }

    @Test
    fun `User with ROLE_SAR_DATA_ACCESS can post subjectAccessRequest`() {
      webTestClient.post()
        .uri("/api/subjectAccessRequest")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS"), name = "INTEGRATION_TEST_USER"))
        .bodyValue("{\"dateFrom\":\"01/01/2001\",\"dateTo\":\"25/12/2022\",\"sarCaseReferenceNumber\":\"mockedCaseReference\",\"services\":\"service1, .com\",\"nomisId\":\"A1111AA\",\"ndeliusId\":null}")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
    }

    @Test
    fun `User without ROLE_SAR_USER_ACCESS can't get subjectAccessRequests`() {
      webTestClient.get()
        .uri("/api/subjectAccessRequests")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS_DENIED")))
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
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
  }
}
