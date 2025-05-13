package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.OverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.RequestTimeoutAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.CreateSubjectAccessRequestException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestApiException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ExtendedSubjectAccessRequestDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequestAdminSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SubjectAccessRequestServiceTest {

  private val subjectAccessRequestRepository: SubjectAccessRequestRepository = mock()
  private val authentication: Authentication = mock()
  private val documentStorageClient: DocumentStorageClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val alertConfiguration: AlertsConfiguration = mock()
  private val requestTimeoutAlertConfig: RequestTimeoutAlertConfiguration = mock()
  private val overdueAlertConfig: OverdueAlertConfiguration = mock()

  @Captor
  private lateinit var sarIdCaptor: ArgumentCaptor<UUID>

  private val subjectAccessRequestService = SubjectAccessRequestService(
    documentStorageClient,
    subjectAccessRequestRepository,
    alertConfiguration,
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
  inner class GetSubjectAccessRequestAdminSummary {
    private val overdueThreshold = LocalDateTime.parse("2024-05-15T15:00:00")

    @BeforeEach()
    fun setUp() {
      whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
      whenever(overdueAlertConfig.calculateOverdueThreshold()).thenReturn(overdueThreshold)
      whenever(subjectAccessRequestRepository.count()).thenReturn(20)
      whenever(subjectAccessRequestRepository.countSubjectAccessRequestsByStatus(Status.Completed)).thenReturn(2)
      whenever(subjectAccessRequestRepository.countSubjectAccessRequestsByStatus(Status.Errored)).thenReturn(3)
      whenever(subjectAccessRequestRepository.countSubjectAccessRequestsByStatusPendingAndNotOverThreshold(overdueThreshold)).thenReturn(4)
      whenever(subjectAccessRequestRepository.countSubjectAccessRequestsByStatusPendingAndOverThredhold(overdueThreshold)).thenReturn(5)
    }

    @Test
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus with no search or pagination when no arguments are given`() {
      whenever(
        subjectAccessRequestRepository.findBySearchTermAndStatus(
          searchTerm = "",
          statuses = setOf(Status.Completed, Status.Errored, Status.Pending),
          pagination = Pageable.unpaged(Sort.by("requestDateTime").descending()),
        ),
      ).thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(true, true, true, true, "", null, null)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "",
        statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
        Pageable.unpaged(Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    @Test
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus method with search string`() {
      whenever(
        subjectAccessRequestRepository.findBySearchTermAndStatus(
          searchTerm = "test",
          statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
          Pageable.unpaged(Sort.by("requestDateTime").descending()),
        ),
      ).thenReturn(Page.empty())

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(true, true, true, true, "test", null, null)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "test",
        statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
        Pageable.unpaged(Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(emptyList()))
    }

    @Test
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus method with requestDateTime-sorted pagination`() {
      whenever(
        subjectAccessRequestRepository.findBySearchTermAndStatus(
          searchTerm = "",
          statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
          PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
        ),
      ).thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(true, true, true, true, "", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "",
        statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    @Test
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus method with search string and requestDateTime-sorted pagination`() {
      whenever(
        subjectAccessRequestRepository.findBySearchTermAndStatus(
          searchTerm = "test",
          statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
          PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
        ),
      ).thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(true, true, true, true, "test", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "test",
        statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "Pending, 2024-05-16T12:00:00, Pending",
        "Pending, 2024-05-15T15:00:01, Pending",
        "Pending, 2024-05-15T15:00:00, Pending",
        "Pending, 2024-05-15T14:59:59, Overdue",
        "Pending, 2024-05-14T15:30:00, Overdue",
        "Completed, 2024-05-15T15:00:01, Completed",
        "Completed, 2024-05-15T15:00:00, Completed",
        "Completed, 2024-05-15T14:59:59, Completed",
        "Errored, 2024-05-15T15:00:01, Errored",
        "Errored, 2024-05-15T15:00:00, Errored",
        "Errored, 2024-05-15T14:59:59, Errored",
      ],
    )
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus method and converts status accordingly`(status: Status, requestDateTime: LocalDateTime, expectedStatus: String) {
      whenever(
        subjectAccessRequestRepository.findBySearchTermAndStatus(
          searchTerm = "test",
          statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
          PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
        ),
      ).thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult(status = status, requestDateTime = requestDateTime)), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(true, true, true, true, "test", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "test",
        statuses = setOf(Status.Completed, Status.Pending, Status.Errored),
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail(status = expectedStatus, requestDateTime = requestDateTime))))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "true, true, true, true, Completed|Pending|Errored",
        "true, false, true, true, Completed|Pending",
        "false, true, true, true, Pending|Errored",
        "false, false, true, true, Pending",
        "true, true, false, false, Completed|Errored",
        "true, false, false, false, Completed",
        "false, true, false, false, Errored",
        "false, false, false, false, ",
      ],
    )
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatus method with different filter options`(completed: Boolean, errored: Boolean, overdue: Boolean, pending: Boolean, expectedStatuses: String?) {
      whenever(subjectAccessRequestRepository.findBySearchTermAndStatus(any(), any(), any()))
        .thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(completed, errored, overdue, pending, "test", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatus(
        searchTerm = "test",
        statuses = convertToStatusSet(expectedStatuses),
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "true, true, true, false, Completed|Pending|Errored",
        "true, false, true, false, Completed|Pending",
        "false, true, true, false, Pending|Errored",
        "false, false, true, false, Pending",
      ],
    )
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatusAndExcludePendingNotOverThreshold method with different filter options`(completed: Boolean, errored: Boolean, overdue: Boolean, pending: Boolean, expectedStatuses: String?) {
      whenever(subjectAccessRequestRepository.findBySearchTermAndStatusAndExcludePendingNotOverThreshold(any(), any(), any(), any()))
        .thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(completed, errored, overdue, pending, "test", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatusAndExcludePendingNotOverThreshold(
        searchTerm = "test",
        statuses = convertToStatusSet(expectedStatuses),
        overdueThreshold = overdueThreshold,
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "true, true, false, true, Completed|Pending|Errored",
        "true, false, false, true, Completed|Pending",
        "false, true, false, true, Pending|Errored",
        "false, false, false, true, Pending",
      ],
    )
    fun `getSubjectAccessRequestAdminSummary calls repository findBySearchTermAndStatusAndExcludePendingOverThreshold method with different filter options`(completed: Boolean, errored: Boolean, overdue: Boolean, pending: Boolean, expectedStatuses: String?) {
      whenever(subjectAccessRequestRepository.findBySearchTermAndStatusAndExcludePendingOverThreshold(any(), any(), any(), any()))
        .thenReturn(PageImpl<SubjectAccessRequest>(listOf(sarResult()), PageRequest.of(0, 1), 1))

      val result = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(completed, errored, overdue, pending, "test", 0, 1)

      verify(subjectAccessRequestRepository).findBySearchTermAndStatusAndExcludePendingOverThreshold(
        searchTerm = "test",
        statuses = convertToStatusSet(expectedStatuses),
        overdueThreshold = overdueThreshold,
        PageRequest.of(0, 1, Sort.by("requestDateTime").descending()),
      )
      assertThat(result).isEqualTo(sarAdminSummaryWithRequests(listOf(extendedSarDetail())))
    }

    private fun sarAdminSummaryWithRequests(requests: List<ExtendedSubjectAccessRequestDetail>) = SubjectAccessRequestAdminSummary(
      totalCount = 20,
      completedCount = 2,
      pendingCount = 4,
      overdueCount = 5,
      erroredCount = 3,
      filterCount = requests.size.toLong(),
      requests = requests,
    )

    private fun convertToStatusSet(csvSet: String?): Set<Status> = csvSet?.split("|")?.map(Status::valueOf)?.toSet() ?: emptySet()

    private fun sarResult(
      status: Status = Status.Completed,
      requestDateTime: LocalDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
    ) = SubjectAccessRequest(
      id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
      status = status,
      dateFrom = LocalDate.parse("2025-01-01"),
      dateTo = LocalDate.parse("2025-03-01"),
      sarCaseReferenceNumber = "123",
      services = "",
      nomisId = "",
      ndeliusCaseReferenceId = "",
      requestedBy = "user",
      requestDateTime = requestDateTime,
      claimDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
      claimAttempts = 0,
      objectUrl = "url",
      lastDownloaded = null,
    )
    private fun extendedSarDetail(
      status: String = "Completed",
      requestDateTime: LocalDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
    ) = ExtendedSubjectAccessRequestDetail(
      id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
      status = status,
      dateFrom = LocalDate.parse("2025-01-01"),
      dateTo = LocalDate.parse("2025-03-01"),
      sarCaseReferenceNumber = "123",
      services = "",
      nomisId = "",
      ndeliusCaseReferenceId = "",
      requestedBy = "user",
      requestDateTime = requestDateTime,
      claimDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
      claimAttempts = 0,
      objectUrl = "url",
      lastDownloaded = null,
    )
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
      val sar: SubjectAccessRequest = mock()

      whenever(sar.status).thenReturn(Status.Pending)

      whenever(subjectAccessRequestRepository.findById(uuid))
        .thenReturn(Optional.of(sar))

      subjectAccessRequestService.completeSubjectAccessRequest(uuid)
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

  @Nested
  inner class FailTimedOutRequests {

    private lateinit var now: LocalDateTime
    private lateinit var threshold: LocalDateTime

    private lateinit var timedOutSar1: SubjectAccessRequest
    private lateinit var timedOutSar2: SubjectAccessRequest
    private lateinit var timedOutSar3: SubjectAccessRequest

    @BeforeEach
    fun setup() {
      now = LocalDateTime.now()
      threshold = now.minusHours(12)
      timedOutSar1 = subjectAccessRequestSubmittedAt(now.minusHours(13), Status.Pending)
      timedOutSar2 = subjectAccessRequestSubmittedAt(now.minusHours(14), Status.Pending)
      timedOutSar3 = subjectAccessRequestSubmittedAt(now.minusHours(21), Status.Pending)

      whenever(alertConfiguration.requestTimeoutAlertConfig).thenReturn(requestTimeoutAlertConfig)

      whenever(requestTimeoutAlertConfig.calculateTimeoutThreshold()).thenReturn(threshold)

      whenever(subjectAccessRequestRepository.findAllPendingSubjectAccessRequestsSubmittedBefore(threshold))
        .thenReturn(listOf(timedOutSar1, timedOutSar2, timedOutSar3))
    }

    @Test
    fun `should set status to Errored for requests submitted before timeout threshold`() {
      whenever(subjectAccessRequestRepository.updateStatusToErrorSubmittedBefore(any(), eq(threshold)))
        .thenReturn(1)

      val result = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()

      assertThat(result).containsExactlyInAnyOrder(
        timedOutSar1,
        timedOutSar2,
        timedOutSar3,
      )

      verify(subjectAccessRequestRepository, times(3)).updateStatusToErrorSubmittedBefore(
        capture(sarIdCaptor),
        eq(threshold),
      )

      assertThat(sarIdCaptor.allValues).hasSize(3)
      assertThat(sarIdCaptor.allValues[0]).isEqualTo(timedOutSar1.id)
      assertThat(sarIdCaptor.allValues[1]).isEqualTo(timedOutSar2.id)
      assertThat(sarIdCaptor.allValues[2]).isEqualTo(timedOutSar3.id)
    }

    @Test
    fun `should not return subject access requests that were not updated successfully`() {
      whenever(subjectAccessRequestRepository.updateStatusToErrorSubmittedBefore(eq(timedOutSar1.id), eq(threshold)))
        .thenReturn(1)
      whenever(subjectAccessRequestRepository.updateStatusToErrorSubmittedBefore(eq(timedOutSar2.id), eq(threshold)))
        .thenReturn(0)
      whenever(subjectAccessRequestRepository.updateStatusToErrorSubmittedBefore(eq(timedOutSar3.id), eq(threshold)))
        .thenReturn(1)

      val result = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()

      assertThat(result).containsExactlyInAnyOrder(
        timedOutSar1,
        timedOutSar3,
      )

      verify(subjectAccessRequestRepository, times(3)).updateStatusToErrorSubmittedBefore(
        capture(sarIdCaptor),
        eq(threshold),
      )

      assertThat(sarIdCaptor.allValues).hasSize(3)
      assertThat(sarIdCaptor.allValues[0]).isEqualTo(timedOutSar1.id)
      assertThat(sarIdCaptor.allValues[1]).isEqualTo(timedOutSar2.id)
      assertThat(sarIdCaptor.allValues[2]).isEqualTo(timedOutSar3.id)
    }

    @Test
    fun `should not update if no timed out subject access requests found`() {
      whenever(subjectAccessRequestRepository.findAllPendingSubjectAccessRequestsSubmittedBefore(threshold))
        .thenReturn(emptyList())

      val result = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()

      assertThat(result).isEmpty()

      verify(subjectAccessRequestRepository, never()).updateStatusToErrorSubmittedBefore(
        any(),
        any(),
      )
    }

    private fun subjectAccessRequestSubmittedAt(submittedAt: LocalDateTime, status: Status) = SubjectAccessRequest(
      id = UUID.randomUUID(),
      status = status,
      dateFrom = dateFromFormatted,
      dateTo = dateToFormatted,
      sarCaseReferenceNumber = "1234abc",
      services = "{1,2,4}",
      nomisId = null,
      ndeliusCaseReferenceId = "1",
      requestedBy = "UserName",
      requestDateTime = submittedAt,
      claimAttempts = 0,
    )
  }

  @Nested
  inner class DuplicateSubjectAccessRequest {
    private val securityContext: SecurityContext = mock()
    private val authentication: Authentication = mock()
    private val authPrincipalName = "Homer Simpson"

    @BeforeEach
    internal fun setup() {
      whenever(authentication.name)
        .thenReturn(authPrincipalName)

      whenever(securityContext.authentication)
        .thenReturn(authentication)

      SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `should throw exception if original request is not found`() {
      val originalId = UUID.randomUUID()

      whenever(subjectAccessRequestRepository.findById(originalId))
        .thenReturn(Optional.empty())

      val exception = assertThrows<SubjectAccessRequestApiException> {
        subjectAccessRequestService.duplicateSubjectAccessRequest(originalId)
      }

      assertThat(exception.message).isEqualTo("duplicate subject access request unsuccessful: request ID not found")
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.subjectAccessRequestId).isEqualTo(originalId.toString())

      verify(subjectAccessRequestRepository, times(1)).findById(eq(originalId))
      verify(subjectAccessRequestRepository, never()).save(any())
    }

    @Test
    fun `should throw expected exception if findById throws exception`() {
      val originalId = UUID.randomUUID()

      whenever(subjectAccessRequestRepository.findById(originalId))
        .thenThrow(RuntimeException("BIG SCARY FIREBALL!!!"))

      val exception = assertThrows<SubjectAccessRequestApiException> {
        subjectAccessRequestService.duplicateSubjectAccessRequest(originalId)
      }

      assertThat(exception.message).isEqualTo("unexpected error occurred while attempting to find request by id")
      assertThat(exception.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      assertThat(exception.subjectAccessRequestId).isEqualTo(originalId.toString())

      verify(subjectAccessRequestRepository, times(1)).findById(eq(originalId))
      verify(subjectAccessRequestRepository, never()).save(any())
    }

    @Test
    fun `should throw expected exception if save throws exception`() {
      val originalRequest = newSubjectAccessRequest()
      whenever(subjectAccessRequestRepository.findById(originalRequest.id))
        .thenReturn(Optional.of(originalRequest))

      whenever(subjectAccessRequestRepository.save(any()))
        .thenThrow(RuntimeException("BIG SCARY FIREBALL!!!"))

      val exception = assertThrows<SubjectAccessRequestApiException> {
        subjectAccessRequestService.duplicateSubjectAccessRequest(originalRequest.id)
      }

      assertThat(exception.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      assertThat(exception.message).isEqualTo("unexpected error occurred while attempting to save subject access request")
      assertThat(exception.subjectAccessRequestId).isNull()

      verify(subjectAccessRequestRepository, times(1)).findById(originalRequest.id)
      verify(subjectAccessRequestRepository, times(1)).save(any())
    }

    @Test
    fun `should successfully resubmit request copying expected fields from original request`() {
      val originalRequest = newSubjectAccessRequest()
      whenever(subjectAccessRequestRepository.findById(originalRequest.id))
        .thenReturn(Optional.of(originalRequest))

      val saveSubjectAccessRequestCaptor: ArgumentCaptor<SubjectAccessRequest> =
        ArgumentCaptor.forClass(SubjectAccessRequest::class.java)

      val actual = subjectAccessRequestService.duplicateSubjectAccessRequest(originalRequest.id)

      assertThat(actual).isNotNull
      assertThat(actual.id).isNotNull()
      assertThat(actual.id).isNotEmpty()
      assertThat(actual.id).isNotEqualTo(originalRequest.id.toString())
      assertThat(actual.originalId).isEqualTo(originalRequest.id.toString())
      assertThat(actual.sarCaseReferenceNumber).isEqualTo(originalRequest.sarCaseReferenceNumber)

      verify(subjectAccessRequestRepository, times(1))
        .findById(originalRequest.id)

      verify(subjectAccessRequestRepository, times(1))
        .save(capture(saveSubjectAccessRequestCaptor))

      assertThat(saveSubjectAccessRequestCaptor.allValues).hasSize(1)
      val savedRequest = saveSubjectAccessRequestCaptor.firstValue

      assertThat(savedRequest).isNotNull
      assertThat(savedRequest.id).isNotEqualTo(originalRequest.id)
      assertThat(savedRequest.id).isEqualTo(UUID.fromString(actual.id))
      assertThat(savedRequest.status).isEqualTo(Status.Pending)
      assertThat(savedRequest.dateFrom).isEqualTo(originalRequest.dateFrom)
      assertThat(savedRequest.dateTo).isEqualTo(originalRequest.dateTo)
      assertThat(savedRequest.sarCaseReferenceNumber).isEqualTo(originalRequest.sarCaseReferenceNumber)
      assertThat(savedRequest.services).isEqualTo(originalRequest.services)
      assertThat(savedRequest.nomisId).isEqualTo(originalRequest.nomisId)
      assertThat(savedRequest.ndeliusCaseReferenceId).isEqualTo(originalRequest.ndeliusCaseReferenceId)
      assertThat(savedRequest.requestedBy).isEqualTo(authPrincipalName)
      assertThat(savedRequest.requestDateTime).isAfter(originalRequest.requestDateTime)
      assertThat(savedRequest.claimAttempts).isEqualTo(0)
      assertThat(savedRequest.claimDateTime).isNull()
      assertThat(savedRequest.objectUrl).isNull()
      assertThat(savedRequest.lastDownloaded).isNull()
    }

    private fun newSubjectAccessRequest() = SubjectAccessRequest(
      dateFrom = LocalDate.now().minusYears(1),
      dateTo = LocalDate.now(),
      sarCaseReferenceNumber = "1234567890",
      services = "service1",
      nomisId = "1",
      ndeliusCaseReferenceId = null,
      requestedBy = "Bob",
    )
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
