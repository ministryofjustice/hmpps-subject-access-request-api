package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SubjectAccessRequestServiceTest {

  private val subjectAccessRequestRepository: SubjectAccessRequestRepository = mock()
  private val authentication: Authentication = mock()
  private val documentStorageClient: DocumentStorageClient = mock()
  private val subjectAccessRequestService = SubjectAccessRequestService(documentStorageClient, subjectAccessRequestRepository)

  private val formattedCurrentTime =
    LocalDateTime.parse("02/01/2024 00:30", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

  @Nested
  inner class CreateSubjectAccessRequest {
    @Test
    fun `createSubjectAccessRequest returns empty string`() {
      whenever(authentication.name).thenReturn("UserName")
      val expected = ""

      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(nDeliusRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo(expected)
      verify(subjectAccessRequestRepository, times(1)).save(sampleSAR)
    }

    @Test
    fun `createSubjectAccessRequest returns error string if both IDs are supplied`() {
      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(nDeliusAndNomisRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo("Both nomisId and nDeliusId are provided - exactly one is required")
      verify(subjectAccessRequestRepository, times(0)).save(sampleSAR)
    }

    @Test
    fun `createSubjectAccessRequest returns error string if neither subject ID is supplied`() {
      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(noIDRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo("Neither nomisId nor nDeliusId is provided - exactly one is required")
      verify(subjectAccessRequestRepository, times(0)).save(sampleSAR)
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateTo is not provided`() {
      whenever(authentication.name).thenReturn("UserName")

      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(noDateToRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo("")
      verify(subjectAccessRequestRepository, times(1)).save(any())
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateFrom is not provided`() {
      whenever(authentication.name).thenReturn("UserName")

      val result: String = subjectAccessRequestService
        .createSubjectAccessRequest(noDateFromRequest, "UserName", requestTime, sampleSAR.id)

      assertThat(result).isEqualTo("")
      verify(subjectAccessRequestRepository, times(1)).save(any())
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
  }

  private val nDeliusRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  private val nDeliusAndNomisRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: '1', " +
    "ndeliusId: '1' " +
    "}"

  private val noIDRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: null " +
    "}"

  private val noDateToRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  private val noDateFromRequest = "{ " +
    "dateFrom: '', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  private val json = JSONObject(nDeliusRequest)
  private val dateFrom = json.get("dateFrom").toString()
  private val dateFromFormatted = LocalDate.parse(dateFrom, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
  private val dateTo = json.get("dateTo").toString()
  private val dateToFormatted = LocalDate.parse(dateTo, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
