package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.data.domain.PageRequest
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
  private val dateTo = "03/01/2024"
  private val dateToFormatted = LocalDate.parse(dateTo, dateFormatter)
  private val requestTime = "01/01/2024 00:00"
  private val requestTimeFormatted = LocalDateTime.parse(requestTime, dateTimeFormatter)

  private val unclaimedSar = SubjectAccessRequest(
    id = testUuid,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 0,
  )

  private val mockSarsWithNoClaims = listOf(unclaimedSar, unclaimedSar, unclaimedSar)
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
  inner class GetSubjectAccessRequests {
    @Test
    fun `calls findAll if unclaimed is false and no filters are specified`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = false, search = "")
      verify(sarRepository, times(1)).findAll()
    }

    @Test
    fun `calls findByClaimAttemptsIs if unclaimed is true and no filters are specified`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, search = "")
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsIs(Status.Pending, 0)
    }

    @Test
    fun `calls findByClaimAttemptsIs(0) if unclaimed is true and no filters are specified`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, search = "")
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsIs(Status.Pending, 0)
    }

    @Test
    fun `calls findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore if unclaimed is true`() {
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val expiredClaimDateTime = "01/01/2024 23:55"
      val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, search = "", formattedMockedCurrentTime)
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)
    }

    @Test
    fun `returns joint list of both claimed and valid unclaimed sars if unclaimed is true`() {
      Mockito.`when`(sarRepository.findByStatusIsAndClaimAttemptsIs(Status.Pending, 0)).thenReturn(mockSarsWithNoClaims)
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val expiredClaimDateTime = "01/01/2024 23:55"
      val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)
      Mockito.`when`(sarRepository.findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)).thenReturn(
        emptyList(),
      )
      val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, search = "", formattedMockedCurrentTime)
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)
      Assertions.assertTrue(result.size == 3)
    }

    @Test
    fun `passes correct parameters and returns filtered list if unclaimed is false and filters are provided`() {
      Mockito.`when`(sarRepository.findFilteredRecords("TEST_REF")).thenReturn(mockSarsWithNoClaims)
      val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = false, search = "TEST_REF")
      verify(sarRepository, times(1)).findFilteredRecords(search = "TEST_REF")
      Assertions.assertTrue(result.size == 3)
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
  inner class GetAllReports {
    @Test
    fun `getReports calls repository findAll method with requestDateTime-sorted pagination`() {
      Mockito.`when`(sarRepository.findAll(PageRequest.of(0, 1))).thenReturn(any())

      SubjectAccessRequestGateway(sarRepository).getAllReports(0, 1)

      verify(sarRepository, times(1)).findAll(PageRequest.of(0, 1, Sort.by("RequestDateTime").descending()))
    }
  }
}
