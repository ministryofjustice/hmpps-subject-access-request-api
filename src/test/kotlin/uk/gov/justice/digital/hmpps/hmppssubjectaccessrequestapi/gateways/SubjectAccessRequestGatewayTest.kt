package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubjectAccessRequestGatewayTest {
  @Nested
  inner class getSubjectAccessRequests {
    private val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    @Test
    fun `calls findAll if unclaimed is false`() {
      val result = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = false)
      verify(sarRepository, times(1)).findAll()
    }

    @Test
    fun `calls findByClaimAttemptsIs if unclaimed is true`() {
      val result = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true)
      verify(sarRepository, times(1)).findByClaimAttemptsIs(0)
    }

    @Test
    fun `calls findByClaimAttemptsIs(0) if unclaimed is true`() {
      val result = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true)
      verify(sarRepository, times(1)).findByClaimAttemptsIs(0)
    }

    @Test
    fun `calls findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore if unclaimed is true`() {
      val mockedCurrentTime = "02/01/2024 00:00"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val expiredClaimDateTime = "01/01/2024 23:55"
      val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)

      val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
        .getSubjectAccessRequests(unclaimedOnly = true, formattedMockedCurrentTime)
      verify(sarRepository, times(1)).findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, expiredClaimDateTimeFormatted)
    }

  }
}
