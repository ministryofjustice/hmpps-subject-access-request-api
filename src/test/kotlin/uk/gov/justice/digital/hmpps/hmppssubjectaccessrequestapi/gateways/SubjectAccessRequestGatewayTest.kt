package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SubjectAccessRequestGatewayTest {
  private val testUuid = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  private val dateFrom = "01/12/2023"
  private val dateFromFormatted = LocalDate.parse(dateFrom, dateFormatter)
  private val requestTime = "01/01/2024 00:00"
  private val requestTimeFormatted = LocalDateTime.parse(requestTime, dateTimeFormatter)

  private val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)

  @Nested
  inner class SaveSubjectAccessRequest {
    @Test
    fun `saves SAR with dateTo of today when dateTo is null`() {
      val sarWithNoDateTo = SubjectAccessRequest(
        id = testUuid,
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = null,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTimeFormatted,
        claimAttempts = 0,
      )

      SubjectAccessRequestGateway(sarRepository)
        .saveSubjectAccessRequest(sarWithNoDateTo)
      verify(sarRepository, times(1)).save(
        SubjectAccessRequest(
          id = testUuid,
          status = Status.Pending,
          dateFrom = dateFromFormatted,
          dateTo = LocalDate.now(),
          sarCaseReferenceNumber = "1234abc",
          services = "{1,2,4}",
          nomisId = "",
          ndeliusCaseReferenceId = "1",
          requestedBy = "Test",
          requestDateTime = requestTimeFormatted,
          claimAttempts = 0,
        ),
      )
    }
  }

  @Nested
  inner class UpdateSubjectAccessRequest {
    @Test
    fun `calls updateClaimDateTimeIfBeforeThreshold with correct parameters`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val thresholdTime = "30/06/2023 00:00"
      val thresholdTimeFormatted = LocalDateTime.parse(thresholdTime, dateTimeFormatter)
      SubjectAccessRequestGateway(sarRepository)
        .updateSubjectAccessRequestClaim(testUuid, thresholdTimeFormatted, formattedMockedCurrentTime)
      verify(sarRepository, times(1)).updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(testUuid, thresholdTimeFormatted, formattedMockedCurrentTime)
    }

    @Test
    fun `calls updateStatus with correct parameters`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val status = Status.Completed
      SubjectAccessRequestGateway(sarRepository)
        .updateSubjectAccessRequestStatusCompleted(testUuid)
      verify(sarRepository, times(1)).updateStatus(testUuid, status)
    }
  }

  @Nested
  inner class GetSubjectAccessRequests {
    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining with no search or pagination when no arguments are given`() {
      Mockito.`when`(sarRepository.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "", nomisSearch = "", ndeliusSearch = "", Pageable.unpaged(Sort.by("RequestDateTime").descending()))).thenReturn(
        Page.empty(),
      )

      SubjectAccessRequestGateway(sarRepository).getSubjectAccessRequests(false, "", null, null)

      verify(sarRepository, times(1)).findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "", nomisSearch = "", ndeliusSearch = "", Pageable.unpaged(Sort.by("RequestDateTime").descending()))
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining method with search string`() {
      Mockito.`when`(sarRepository.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "test", nomisSearch = "test", ndeliusSearch = "test", Pageable.unpaged(Sort.by("RequestDateTime").descending()))).thenReturn(
        Page.empty(),
      )

      SubjectAccessRequestGateway(sarRepository).getSubjectAccessRequests(false, "test", null, null)

      verify(sarRepository, times(1)).findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "test", nomisSearch = "test", ndeliusSearch = "test", Pageable.unpaged(Sort.by("RequestDateTime").descending()))
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining method with requestDateTime-sorted pagination`() {
      Mockito.`when`(sarRepository.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "", nomisSearch = "", ndeliusSearch = "", PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()))).thenReturn(
        Page.empty(),
      )

      SubjectAccessRequestGateway(sarRepository).getSubjectAccessRequests(false, "", 0, 1)

      verify(sarRepository, times(1)).findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "", nomisSearch = "", ndeliusSearch = "", PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()))
    }

    @Test
    fun `getSubjectAccessRequests calls repository findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining method with search string and requestDateTime-sorted pagination`() {
      Mockito.`when`(sarRepository.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "test", nomisSearch = "test", ndeliusSearch = "test", PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()))).thenReturn(
        Page.empty(),
      )

      SubjectAccessRequestGateway(sarRepository).getSubjectAccessRequests(false, "test", 0, 1)

      verify(sarRepository, times(1)).findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch = "test", nomisSearch = "test", ndeliusSearch = "test", PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()))
    }

    @Test
    fun `getSubjectAccessRequests calls repository findUnclaimed if unclaimedOnly is true`() {
      Mockito.`when`(sarRepository.findUnclaimed(claimDateTime = requestTimeFormatted.minusMinutes(5))).thenReturn(
        emptyList(),
      )

      SubjectAccessRequestGateway(sarRepository).getSubjectAccessRequests(true, "", null, null, currentTime = requestTimeFormatted)

      verify(sarRepository, times(1)).findUnclaimed(claimDateTime = requestTimeFormatted.minusMinutes(5))
    }
  }

  @Nested
  inner class UpdateLastDownloaded {
    @Test
    fun `calls updateLastDownloaded with correct parameters`() {
      val downloadTime = LocalDateTime.parse("01/07/2024 00:00", dateTimeFormatter)

      SubjectAccessRequestGateway(sarRepository)
        .updateLastDownloadedDateTime(testUuid, downloadTime)

      verify(sarRepository, times(1)).updateLastDownloaded(testUuid, downloadTime)
    }
  }

  @Nested
  inner class DeleteOldSubjectAccessRequests {
    @Test
    fun `calls findByRequestDateTimeBefore with 7 days threshold`() {
      val timeNow = LocalDateTime.now()
      SubjectAccessRequestGateway(sarRepository)
        .getOldSubjectAccessRequests(timeNow)

      val thresholdTime = timeNow.minusDays(7)

      verify(sarRepository, times(1)).findByRequestDateTimeBefore(thresholdTime)
    }
  }
}
