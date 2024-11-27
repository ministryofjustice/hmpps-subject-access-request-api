package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.CreateSubjectAccessRequestException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SubjectAccessRequestControllerTest {
  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val authentication: Authentication = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val alertsConfiguration: AlertsConfiguration = mock()
  private val subjectAccessRequestController = SubjectAccessRequestController(
    subjectAccessRequestService = subjectAccessRequestService,
    telemetryClient = telemetryClient,
    alertsConfiguration = alertsConfiguration,
  )
  private val requestTime = LocalDateTime.now()

  private val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")

  @Nested
  inner class CreateSubjectAccessRequest {
    @Test
    fun `createSubjectAccessRequest post calls service createSubjectAccessRequest with same parameters`() {
      val ndeliusRequest = CreateSubjectAccessRequestEntity(
        nomisId = null,
        ndeliusId = "1",
        services = "{1,2,4}",
        sarCaseReferenceNumber = "1234abc",
        dateFrom = LocalDate.of(2023, 12, 1),
        dateTo = LocalDate.of(2024, 12, 3),
      )

      whenever(authentication.name).thenReturn("UserName")
      whenever(
        subjectAccessRequestService.createSubjectAccessRequest(
          ndeliusRequest,
          "UserName",
          requestTime,
        ),
      ).thenReturn("")
      val expected: ResponseEntity<String> = ResponseEntity("", HttpStatus.CREATED)

      val result = subjectAccessRequestController
        .createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)

      verify(subjectAccessRequestService, times(1)).createSubjectAccessRequest(ndeliusRequest, "UserName", requestTime)
      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequest throws expected exception if both nomis and ndelius ids are provided`() {
      whenever(authentication.name).thenReturn("UserName")

      val ndeliusAndNomisRequest = CreateSubjectAccessRequestEntity(
        nomisId = "1",
        ndeliusId = "1",
        services = "{1,2,4}",
        sarCaseReferenceNumber = "1234abc",
        dateFrom = LocalDate.of(2023, 12, 1),
        dateTo = LocalDate.of(2024, 12, 3),
      )

      whenever(subjectAccessRequestService.createSubjectAccessRequest(ndeliusAndNomisRequest, "UserName", requestTime))
        .thenThrow(
          CreateSubjectAccessRequestException(
            "Both nomisId and nDeliusId are provided - exactly one is required",
            HttpStatus.BAD_REQUEST,
          ),
        )

      val exception = assertThrows<CreateSubjectAccessRequestException> {
        subjectAccessRequestController.createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)
      }

      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.message).isEqualTo("Both nomisId and nDeliusId are provided - exactly one is required")

      verify(subjectAccessRequestService, times(1))
        .createSubjectAccessRequest(ndeliusAndNomisRequest, "UserName", requestTime)
    }

    @Test
    fun `createSubjectAccessRequest throws expected exception if neither nomis nor ndelius ids are provided`() {
      val noIDRequest = CreateSubjectAccessRequestEntity(
        nomisId = null,
        ndeliusId = null,
        services = "{1,2,4}",
        sarCaseReferenceNumber = "1234abc",
        dateFrom = LocalDate.of(2023, 12, 1),
        dateTo = LocalDate.of(2024, 12, 3),
      )

      whenever(subjectAccessRequestService.createSubjectAccessRequest(noIDRequest, "UserName", requestTime))
        .thenThrow(
          CreateSubjectAccessRequestException(
            "Neither nomisId nor ndeliusId is provided - exactly one is required",
            HttpStatus.BAD_REQUEST,
          ),
        )
      whenever(authentication.name).thenReturn("UserName")

      val exception = assertThrows<CreateSubjectAccessRequestException> {
        subjectAccessRequestController.createSubjectAccessRequest(noIDRequest, authentication, requestTime)
      }

      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.message).isEqualTo("Neither nomisId nor ndeliusId is provided - exactly one is required")

      verify(subjectAccessRequestService, times(1))
        .createSubjectAccessRequest(noIDRequest, "UserName", requestTime)
    }
  }

  @Nested
  inner class GetSubjectAccessRequests {
    @Test
    fun `getSubjectAccessRequests is called with unclaimedOnly, search and pagination parameters if specified in controller and returns list`() {
      val result: List<SubjectAccessRequest?> =
        subjectAccessRequestController
          .getSubjectAccessRequests(unclaimed = true, search = "testSearchString", pageNumber = 1, pageSize = 1)

      verify(subjectAccessRequestService, times(1)).getSubjectAccessRequests(
        unclaimedOnly = true,
        search = "testSearchString",
        pageNumber = 1,
        pageSize = 1,
      )
      Assertions.assertThatList(result)
    }

    @Test
    fun `getSubjectAccessRequests is called with unclaimedOnly = false, search = '' and no pagination parameters if unspecified in controller`() {
      subjectAccessRequestController
        .getSubjectAccessRequests()

      verify(subjectAccessRequestService, times(1)).getSubjectAccessRequests(
        unclaimedOnly = false,
        search = "",
        pageNumber = null,
        pageSize = null,
      )
    }
  }

  @Nested
  inner class GetTotalSubjectAccessRequests {
    @Test
    fun `getTotalSubjectAccessRequests calls getSubjectAccessRequests with unclaimedOnly = false `() {
      subjectAccessRequestController
        .getTotalSubjectAccessRequests()
      verify(subjectAccessRequestService, times(1)).getSubjectAccessRequests(unclaimedOnly = false, search = "")
    }
  }

  @Nested
  inner class PatchSubjectAccessRequest {
    @Test
    fun `claimSubjectAccessRequest returns Bad Request if updateSubjectAccessRequest returns 0 with claim time update`() {
      whenever(subjectAccessRequestService.claimSubjectAccessRequest(eq(testUuid))).thenReturn(0)

      val result = subjectAccessRequestController
        .claimSubjectAccessRequest(testUuid)

      verify(subjectAccessRequestService, times(1)).claimSubjectAccessRequest(eq(testUuid))
      assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `claimSubjectAccessRequest returns Response OK if updateSubjectAccessRequest returns 1 with time update`() {
      whenever(subjectAccessRequestService.claimSubjectAccessRequest(eq(testUuid))).thenReturn(1)

      val result = subjectAccessRequestController
        .claimSubjectAccessRequest(testUuid)

      verify(subjectAccessRequestService, times(1)).claimSubjectAccessRequest(eq(testUuid))
      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `completeSubjectAccessRequest returns Bad Request if completeSubjectAccessRequest returns 0 with status update`() {
      whenever(subjectAccessRequestService.completeSubjectAccessRequest(testUuid)).thenReturn(0)

      val result = subjectAccessRequestController
        .completeSubjectAccessRequest(testUuid)

      verify(subjectAccessRequestService, times(1)).completeSubjectAccessRequest(testUuid)
      assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `completeSubjectAccessRequest returns Response OK if completeSubjectAccessRequest returns 1 with status update`() {
      whenever(subjectAccessRequestService.completeSubjectAccessRequest(testUuid)).thenReturn(1)

      val result = subjectAccessRequestController
        .completeSubjectAccessRequest(testUuid)

      verify(subjectAccessRequestService, times(1)).completeSubjectAccessRequest(testUuid)
      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    }
  }

  @Nested
  inner class GetReport {
    @Test
    fun `getReport returns 200 if service retrieveSubjectAccessRequestDocument returns a response`() {
      val mockByteArrayInputStream = Mockito.mock(ByteArrayInputStream::class.java)
      val mockStream = Flux.just(InputStreamResource(mockByteArrayInputStream))
      whenever(
        subjectAccessRequestService.retrieveSubjectAccessRequestDocument(
          sarId = eq(testUuid),
          downloadDateTime = any(),
        ),
      )
        .thenReturn(ResponseEntity.ok().contentType(MediaType.parseMediaType("application/pdf")).body(mockStream))

      val result = subjectAccessRequestController.getReport(testUuid)

      verify(subjectAccessRequestService, times(1)).retrieveSubjectAccessRequestDocument(
        sarId = eq(testUuid),
        downloadDateTime = any(),
      )
      assertThat(result).isEqualTo(
        ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("application/pdf"))
          .body(InputStreamResource(mockByteArrayInputStream)),
      )
    }

    @Test
    fun `getReport returns 404 if service retrieveSubjectAccessRequestDocument does not return a response`() {
      val errorMessage = "Report Not Found"
      whenever(
        subjectAccessRequestService.retrieveSubjectAccessRequestDocument(
          sarId = eq(testUuid),
          downloadDateTime = any(),
        ),
      )
        .thenReturn(null)

      val result = subjectAccessRequestController.getReport(testUuid)

      verify(subjectAccessRequestService, times(1)).retrieveSubjectAccessRequestDocument(
        sarId = eq(testUuid),
        downloadDateTime = any(),
      )
      assertThat(result).isEqualTo(
        ResponseEntity(errorMessage, HttpStatus.NOT_FOUND),
      )
    }
  }
}
