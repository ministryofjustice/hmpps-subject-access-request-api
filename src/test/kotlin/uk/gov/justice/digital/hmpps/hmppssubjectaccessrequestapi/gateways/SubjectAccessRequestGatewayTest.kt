package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SubjectAccessRequestGatewayTest {

  val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
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
  inner class getSubjectAccessRequests {

    @Test
    fun `calls findAll if unclaimed is false`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = false)
      verify(sarRepository, times(1)).findAll()
    }

    @Test
    fun `calls findByClaimAttemptsIs if unclaimed is true`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true)
      verify(sarRepository, times(1)).findByClaimAttemptsIs(0)
    }

    @Test
    fun `calls findByClaimAttemptsIs(0) if unclaimed is true`() {
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true)
      verify(sarRepository, times(1)).findByClaimAttemptsIs(0)
    }

    @Test
    fun `calls findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore if unclaimed is true`() {
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val expiredClaimDateTime = "01/01/2024 23:55"
      val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)
      SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, formattedMockedCurrentTime)
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)
    }

    @Test
    fun `returns joint list of both claimed and valid unclaimed sars`() {
      Mockito.`when`(sarRepository.findByClaimAttemptsIs(0)).thenReturn(mockSarsWithNoClaims)
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val expiredClaimDateTime = "01/01/2024 23:55"
      val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)
      Mockito.`when`(sarRepository.findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)).thenReturn(
        emptyList(),
      )
      val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, formattedMockedCurrentTime)
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)
      Assertions.assertTrue(result.size == 3)
    }
  }

  @Nested
  inner class updateSubjectAccessRequest {
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
  inner class getReports {
    @Test
    fun `getReports calls document gateway findAll method with pagination`() {
      Mockito.`when`(sarRepository.findAll(PageRequest.of(0,1))).thenReturn(any())
      SubjectAccessRequestGateway(sarRepository).getAllReports(PageRequest.of(0, 1))
      verify(sarRepository, times(1)).findAll(PageRequest.of(0,1))
    }
  }
}
