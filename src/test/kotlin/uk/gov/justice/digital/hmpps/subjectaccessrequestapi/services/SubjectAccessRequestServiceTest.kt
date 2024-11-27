package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.CreateSubjectAccessRequestException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SubjectAccessRequestServiceTest {

  private val subjectAccessRequestRepository: SubjectAccessRequestRepository = mock()
  private val authentication: Authentication = mock()
  private val documentStorageClient: DocumentStorageClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val overdueAlertConfiguration: AlertsConfiguration = mock()

  private val subjectAccessRequestService = SubjectAccessRequestService(
    documentStorageClient,
    subjectAccessRequestRepository,
    overdueAlertConfiguration,
    telemetryClient,
  )

  private val formattedCurrentTime =
    LocalDateTime.parse("02/01/2024 00:30", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

  @Nested
  inner class CreateSubjectAccessRequest {
    @Test
    fun `createSubjectAccessRequest returns empty string`() {
      whenever(authentication.name).thenReturn("UserName")

      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(nDeliusRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo(sampleSAR.id.toString())
      verify(subjectAccessRequestRepository, times(1)).save(sampleSAR)
    }

    @Test
    fun `createSubjectAccessRequest returns error string if both IDs are supplied`() {
      val exception = assertThrows<CreateSubjectAccessRequestException> {
        subjectAccessRequestService.createSubjectAccessRequest(
          request = nDeliusAndNomisRequest,
          requestedBy = "UserName",
          requestTime = requestTime,
          id = sampleSAR.id,
        )
      }

      assertThat(exception.message).isEqualTo("Both nomisId and nDeliusId are provided - exactly one is required")
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)

      verify(subjectAccessRequestRepository, never()).save(any())
    }

    @Test
    fun `createSubjectAccessRequest returns error string if neither subject ID is supplied`() {
      val exception = assertThrows<CreateSubjectAccessRequestException> {
        subjectAccessRequestService.createSubjectAccessRequest(
          request = noIDRequest,
          requestedBy = "UserName",
          requestTime = requestTime,
          id = sampleSAR.id,
        )
      }

      assertThat(exception.message).isEqualTo("Neither nomisId or nDeliusId provided - exactly one is required")
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)

      verify(subjectAccessRequestRepository, never()).save(any())
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateTo is not provided`() {
      whenever(authentication.name).thenReturn("UserName")

      val actual = subjectAccessRequestService.createSubjectAccessRequest(
        request = noDateToRequest,
        requestedBy = "UserName",
        requestTime = requestTime,
        id = sampleSAR.id,
      )

      val sarCaptor: ArgumentCaptor<SubjectAccessRequest> = ArgumentCaptor.forClass(SubjectAccessRequest::class.java)

      assertThat(actual).isEqualTo(sampleSAR.id.toString())
      verify(subjectAccessRequestRepository, times(1)).save(capture(sarCaptor))

      assertThat(sarCaptor.allValues.size).isEqualTo(1)
      assertThat(sarCaptor.allValues[0]).isNotNull

      val savedSar = sarCaptor.allValues[0]
      assertThat(savedSar.id).isEqualTo(sampleSAR.id)
      assertThat(savedSar.status).isEqualTo(sampleSAR.status)
      assertThat(savedSar.dateFrom).isEqualTo(sampleSAR.dateFrom)
      assertThat(savedSar.dateTo).isEqualTo(LocalDate.now())
      assertThat(savedSar.sarCaseReferenceNumber).isEqualTo(sampleSAR.sarCaseReferenceNumber)
      assertThat(savedSar.services).isEqualTo(sampleSAR.services)
      assertThat(savedSar.nomisId).isEqualTo(sampleSAR.nomisId)
      assertThat(savedSar.ndeliusCaseReferenceId).isEqualTo(sampleSAR.ndeliusCaseReferenceId)
      assertThat(savedSar.requestedBy).isEqualTo(sampleSAR.requestedBy)
      assertThat(savedSar.requestDateTime).isEqualTo(sampleSAR.requestDateTime)
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateFrom is not provided`() {
      val sarCaptor: ArgumentCaptor<SubjectAccessRequest> = ArgumentCaptor.forClass(SubjectAccessRequest::class.java)

      whenever(authentication.name).thenReturn("UserName")

      val actual = subjectAccessRequestService.createSubjectAccessRequest(
        request = noDateFromRequest,
        requestedBy = "UserName",
        requestTime = requestTime,
        id = sampleSAR.id,
      )

      verify(subjectAccessRequestRepository, times(1)).save(capture(sarCaptor))

      assertThat(actual).isEqualTo(sampleSAR.id.toString())
      assertThat(sarCaptor.allValues.size).isEqualTo(1)
      assertThat(sarCaptor.allValues[0]).isNotNull

      val savedSar = sarCaptor.allValues[0]
      assertThat(savedSar.id).isEqualTo(sampleSAR.id)
      assertThat(savedSar.status).isEqualTo(sampleSAR.status)
      assertThat(savedSar.dateFrom).isNull()
      assertThat(savedSar.dateTo).isEqualTo(savedSar.dateTo)
      assertThat(savedSar.sarCaseReferenceNumber).isEqualTo(sampleSAR.sarCaseReferenceNumber)
      assertThat(savedSar.services).isEqualTo(sampleSAR.services)
      assertThat(savedSar.nomisId).isEqualTo(sampleSAR.nomisId)
      assertThat(savedSar.ndeliusCaseReferenceId).isEqualTo(sampleSAR.ndeliusCaseReferenceId)
      assertThat(savedSar.requestedBy).isEqualTo(sampleSAR.requestedBy)
      assertThat(savedSar.requestDateTime).isEqualTo(sampleSAR.requestDateTime)
    }
  }

  @Nested
  inner class GetSubjectAccessRequest {
    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase with no search or pagination when no arguments are given`() {
      whenever(
        subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
          caseReferenceSearch = "",
          nomisSearch = "",
          ndeliusSearch = "",
          Pageable.unpaged(Sort.by("RequestDateTime").descending()),
        ),
      ).thenReturn(
        Page.empty(),
      )

      subjectAccessRequestService.getSubjectAccessRequests(false, "", null, null)

      verify(
        subjectAccessRequestRepository,
        times(1),
      ).findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
        caseReferenceSearch = "",
        nomisSearch = "",
        ndeliusSearch = "",
        Pageable.unpaged(Sort.by("RequestDateTime").descending()),
      )
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase method with search string`() {
      whenever(
        subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
          caseReferenceSearch = "test",
          nomisSearch = "test",
          ndeliusSearch = "test",
          Pageable.unpaged(Sort.by("RequestDateTime").descending()),
        ),
      ).thenReturn(
        Page.empty(),
      )

      subjectAccessRequestService.getSubjectAccessRequests(false, "test", null, null)

      verify(
        subjectAccessRequestRepository,
        times(1),
      ).findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
        caseReferenceSearch = "test",
        nomisSearch = "test",
        ndeliusSearch = "test",
        Pageable.unpaged(Sort.by("RequestDateTime").descending()),
      )
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase method with requestDateTime-sorted pagination`() {
      whenever(
        subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
          caseReferenceSearch = "",
          nomisSearch = "",
          ndeliusSearch = "",
          PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()),
        ),
      ).thenReturn(
        Page.empty(),
      )

      subjectAccessRequestService.getSubjectAccessRequests(false, "", 0, 1)

      verify(
        subjectAccessRequestRepository,
        times(1),
      ).findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
        caseReferenceSearch = "",
        nomisSearch = "",
        ndeliusSearch = "",
        PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()),
      )
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase method with search string and requestDateTime-sorted pagination`() {
      whenever(
        subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
          caseReferenceSearch = "test",
          nomisSearch = "test",
          ndeliusSearch = "test",
          PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()),
        ),
      ).thenReturn(
        Page.empty(),
      )

      subjectAccessRequestService.getSubjectAccessRequests(false, "test", 0, 1)

      verify(
        subjectAccessRequestRepository,
        times(1),
      ).findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
        caseReferenceSearch = "test",
        nomisSearch = "test",
        ndeliusSearch = "test",
        PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()),
      )
    }

    @Test
    fun `getSubjectAccessRequests calls repository findUnclaimed if unclaimedOnly is true`() {
      whenever(subjectAccessRequestRepository.findUnclaimed(any())).thenReturn(
        emptyList(),
      )

      subjectAccessRequestService.getSubjectAccessRequests(true, "", null, null)

      verify(subjectAccessRequestRepository, times(1)).findUnclaimed(
        argThat { it ->
          it.isBefore(
            LocalDateTime.now().minusMinutes(29),
          )
        },
      )
    }
  }

  @Nested
  inner class UpdateSubjectAccessRequest {

    @Test
    fun `claimSubjectAccessRequest calls repository update method with time 30 minutes ago`() {
      val uuid = UUID.fromString("55555555-5555-5555-5555-555555555555")

      subjectAccessRequestService.claimSubjectAccessRequest(uuid)

      verify(subjectAccessRequestRepository, times(1)).updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
        eq(uuid),
        argThat { it -> it.isBefore(LocalDateTime.now().minusMinutes(29)) },
        argThat { it -> it.isAfter(LocalDateTime.now().minusMinutes(1)) },
      )
    }

    @Test
    fun `completeSubjectAccessRequest calls repository with update status`() {
      val uuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      subjectAccessRequestService
        .completeSubjectAccessRequest(uuid)
      verify(subjectAccessRequestRepository, times(1)).updateStatus(uuid, Status.Completed)
    }
  }

  @Nested
  inner class DocumentRetrieval {
    private val uuid = UUID.randomUUID()

    @Test
    fun `retrieveSubjectAccessRequestDocument calls document storage client retrieve method with id`() {
      subjectAccessRequestService.retrieveSubjectAccessRequestDocument(uuid)
      verify(documentStorageClient, times(1)).retrieveDocument(uuid)
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument returns ResponseEntity if response from document storage is provided`() {
      val byteArrayInputStream: ByteArrayInputStream = mock()
      val stream = Flux.just(InputStreamResource(byteArrayInputStream))
      whenever(documentStorageClient.retrieveDocument(uuid)).thenReturn(ResponseEntity.ok().body(stream))
      val result = subjectAccessRequestService.retrieveSubjectAccessRequestDocument(uuid)
      verify(documentStorageClient, times(1)).retrieveDocument(uuid)
      assertThat(result).isEqualTo(ResponseEntity.ok().body(stream))
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument returns null if response from document storage is not provided`() {
      whenever(documentStorageClient.retrieveDocument(uuid)).thenReturn(null)
      val result = subjectAccessRequestService.retrieveSubjectAccessRequestDocument(uuid)
      verify(documentStorageClient, times(1)).retrieveDocument(uuid)
      assertThat(result).isEqualTo(null)
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument calls SAR repository updateLastDownloadedDateTime method with id and dateTime`() {
      subjectAccessRequestService.retrieveSubjectAccessRequestDocument(uuid, formattedCurrentTime)

      verify(subjectAccessRequestRepository, times(1)).updateLastDownloaded(uuid, formattedCurrentTime)
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument calls telemetryClient trackApiEvent method with ReportDocumentDownloadTimeUpdated`() {
      subjectAccessRequestService.retrieveSubjectAccessRequestDocument(uuid, formattedCurrentTime)

      verify(telemetryClient, times(1)).trackApiEvent(
        "ReportDocumentDownloadTimeUpdated",
        uuid.toString(),
        "downloadDateTime" to formattedCurrentTime.toString(),
      )
    }
  }

  private val nDeliusRequest = CreateSubjectAccessRequestEntity(
    nomisId = null,
    ndeliusId = "1",
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    dateFrom = LocalDate.of(2023, 12, 1),
    dateTo = LocalDate.of(2024, 1, 3),
  )

  private val nDeliusAndNomisRequest = CreateSubjectAccessRequestEntity(
    nomisId = "1",
    ndeliusId = "1",
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    dateFrom = LocalDate.of(2023, 12, 1),
    dateTo = LocalDate.of(2024, 1, 3),
  )

  private val noIDRequest = CreateSubjectAccessRequestEntity(
    nomisId = null,
    ndeliusId = null,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    dateFrom = LocalDate.of(2023, 12, 1),
    dateTo = LocalDate.of(2024, 1, 3),
  )

  private val noDateToRequest = CreateSubjectAccessRequestEntity(
    nomisId = null,
    ndeliusId = "1",
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    dateFrom = LocalDate.of(2023, 12, 1),
    dateTo = null,
  )

  private val noDateFromRequest = CreateSubjectAccessRequestEntity(
    nomisId = null,
    ndeliusId = "1",
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    dateFrom = null,
    dateTo = LocalDate.of(2024, 1, 3),
  )

  private val dateFromFormatted = nDeliusRequest.dateFrom
  private val dateToFormatted = nDeliusRequest.dateTo
  private val requestTime = LocalDateTime.now()
  private val sampleSAR = SubjectAccessRequest(
    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = null,
    ndeliusCaseReferenceId = "1",
    requestedBy = "UserName",
    requestDateTime = requestTime,
    claimAttempts = 0,
  )
}
