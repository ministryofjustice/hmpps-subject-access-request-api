package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DataJpaTest
class SubjectAccessRequestRepositoryTest {
  @Autowired
  private val sarRepository: SubjectAccessRequestRepository? = null

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

  private val dateFrom = "01/12/2023"
  private val dateFromFormatted = LocalDate.parse(dateFrom, dateFormatter)
  private val dateTo = "03/01/2024"
  private val dateToFormatted = LocalDate.parse(dateTo, dateFormatter)
  private val requestTime = "01/01/2024 00:00"
  private val requestTimeFormatted = LocalDateTime.parse(requestTime, dateTimeFormatter)
  private val expiredClaimDateTime = "02/01/2024 00:00"
  private val expiredClaimDateTimeFormatted = LocalDateTime.parse(expiredClaimDateTime, dateTimeFormatter)
  private val currentClaimDateTime = "03/01/2024 00:00"
  private val currentClaimDateTimeFormatted = LocalDateTime.parse(currentClaimDateTime, dateTimeFormatter)


  val unclaimedSar = SubjectAccessRequest(
    id = null,
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
  val pendingSarWithExpiredClaim = SubjectAccessRequest(
    id = null,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 1,
    claimDateTime = expiredClaimDateTimeFormatted,
  )
  val pendingSarWithCurrentClaim = SubjectAccessRequest(
    id = null,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 1,
    claimDateTime = currentClaimDateTimeFormatted,
  )
  val completedSarWithCurrentClaim = SubjectAccessRequest(
    id = null,
    status = Status.Completed, // here
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 1,
    claimDateTime = currentClaimDateTimeFormatted,
  )
  val completedSarWithExpiredClaim = SubjectAccessRequest(
    id = null,
    status = Status.Completed, // here
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 1,
    claimDateTime = expiredClaimDateTimeFormatted,
  )

  @Nested
  inner class findByClaimAttemptsIs {
    @Test
    fun `findByClaimAttemptsIs returns only unclaimed SAR entries if called with 0`() {
      val expectedAll: List<SubjectAccessRequest> = listOf(unclaimedSar, pendingSarWithExpiredClaim)
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar)
      sarRepository?.save(unclaimedSar)
      sarRepository?.save(pendingSarWithExpiredClaim)
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(expectedAll)
      Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(0)).isEqualTo(expectedUnclaimed)
    }

    @Test
    fun `findByClaimAttemptsIs returns only claimed SAR entries if called with 1 or more`() {
      val expectedAll: List<SubjectAccessRequest> = listOf(pendingSarWithExpiredClaim, unclaimedSar)
      val expectedClaimed: List<SubjectAccessRequest> = listOf(pendingSarWithExpiredClaim)
      sarRepository?.save(pendingSarWithExpiredClaim)
      sarRepository?.save(unclaimedSar)
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(expectedAll)
      Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(1)).isEqualTo(expectedClaimed)
    }

    @Test
    fun `db doesn't save between tests`() {
      val emptyList: List<Any> = emptyList()
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(emptyList)
    }
  }

  @Nested
  inner class findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore {
    @Test
    fun `returns only SAR entries with pending status and expired claim date-time if called with pending, 0 and old date-time`() {
      val expectAll: List<SubjectAccessRequest> = listOf(unclaimedSar, unclaimedSar)
    }
  }
}
