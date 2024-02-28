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
import java.util.*

@DataJpaTest
class SubjectAccessRequestRepositoryTest {
  @Autowired
  private val sarRepository: SubjectAccessRequestRepository? = null
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  private val dateFrom = "30/12/2023"
  private val dateFromFormatted = LocalDate.parse(dateFrom, dateFormatter)
  private val dateTo = "30/01/2024"
  private val dateToFormatted = LocalDate.parse(dateTo, dateFormatter)
  private val requestTime = "30/01/2024 00:00"
  private val requestTimeFormatted = LocalDateTime.parse(requestTime, dateTimeFormatter)
  private val claimDateTime = "30/01/2024 00:00"
  private val claimDateTimeFormatted = LocalDateTime.parse(claimDateTime, dateTimeFormatter)
  private val claimDateTimeEarlier = "30/01/2023 00:00"
  private val claimDateTimeEarlierFormatted = LocalDateTime.parse(claimDateTimeEarlier, dateTimeFormatter)

  val unclaimedSar = SubjectAccessRequest(
    id = null,// UUID.fromString("11111111-1111-1111-1111-111111111111"),
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
  val claimedSarWithPendingStatus = SubjectAccessRequest(
    id = null,// UUID.fromString("22222222-2222-2222-2222-222222222222"),
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
    claimDateTime = claimDateTimeFormatted,
  )
  val completedSar = SubjectAccessRequest(
    id = null,// UUID.fromString("33333333-3333-3333-3333-333333333333"),
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
    claimDateTime = claimDateTimeFormatted,
  )
  val sarWithPendingStatusClaimedEarlier = SubjectAccessRequest(
    id = null,// UUID.fromString("44444444-4444-4444-4444-444444444444"),
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
    claimDateTime = claimDateTimeEarlierFormatted,
  )

  fun databaseInsert() {
    sarRepository?.save(unclaimedSar)
    sarRepository?.save(claimedSarWithPendingStatus)
    sarRepository?.save(completedSar)
    sarRepository?.save(sarWithPendingStatusClaimedEarlier)
  }

  val allSars = listOf(unclaimedSar, claimedSarWithPendingStatus, completedSar, sarWithPendingStatusClaimedEarlier)

  @Nested
  inner class findByClaimAttemptsIs {
    @Test
    fun `findByClaimAttemptsIs returns only unclaimed SAR entries if called with 0`() {
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar)
      databaseInsert()
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(0)).isEqualTo(expectedUnclaimed)
    }

    @Test
    fun `findByClaimAttemptsIs returns only claimed SAR entries if called with 1 or more`() {
      val expectedClaimed: List<SubjectAccessRequest> =
        listOf(claimedSarWithPendingStatus, completedSar, sarWithPendingStatusClaimedEarlier)
      databaseInsert()
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(1)).isEqualTo(expectedClaimed)
    }
  }

  @Nested
  inner class findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore {
    @Test
    fun `returns only SAR entries with given criteria`() {
      val expectedPendingClaimedBefore: List<SubjectAccessRequest> = listOf(sarWithPendingStatusClaimedEarlier)
      databaseInsert()
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(
        sarRepository?.findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(
          Status.Pending,
          0,
          claimDateTimeFormatted,
        ),
      ).isEqualTo(expectedPendingClaimedBefore)
    }
  }

  @Nested
  inner class updateSubjectAccessRequestIfClaimDateTimeLessThanWithClaimDateTimeIsAndClaimAttemptsIs {
    @Test
    fun `updates claimDateTime and claimAttempts if claimDateTime before threshold`() {
      val thresholdClaimDateTime = "30/06/2023 00:00"
      val thresholdClaimDateTimeFormatted = LocalDateTime.parse(thresholdClaimDateTime, dateTimeFormatter)
      val currentDateTime = "01/02/2024 00:00"
      val currentDateTimeFormatted = LocalDateTime.parse(currentDateTime, dateTimeFormatter)

      databaseInsert()

      val idOfSarWithPendingStatusClaimedEarlier = UUID.fromString("44444444-4444-4444-4444-444444444444")
      val expectedUpdatedRecord = SubjectAccessRequest(
        id = idOfSarWithPendingStatusClaimedEarlier,
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTimeFormatted,
        claimAttempts = 2,
        claimDateTime = currentDateTimeFormatted,
      )

      var numberOfDbRecordsUpdated = 0
      if (idOfSarWithPendingStatusClaimedEarlier != null) {
        numberOfDbRecordsUpdated = sarRepository?.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
          idOfSarWithPendingStatusClaimedEarlier,
          thresholdClaimDateTimeFormatted,
          currentDateTimeFormatted,
        )!!
      }

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(idOfSarWithPendingStatusClaimedEarlier))
        .isEqualTo(expectedUpdatedRecord)
    }

    @Test
    fun `does not update claimDateTime or claimAttempts if claimDateTime after threshold`() {
      val thresholdClaimDateTime = "30/06/2023 00:00"
      val thresholdClaimDateTimeFormatted = LocalDateTime.parse(thresholdClaimDateTime, dateTimeFormatter)
      val currentDateTime = "01/02/2024 00:00"
      val currentDateTimeFormatted = LocalDateTime.parse(currentDateTime, dateTimeFormatter)

      databaseInsert()

      val idOfClaimedSarWithPendingStatusAfterThreshold: UUID = UUID.randomUUID() //sarRepository?.findAll()?.first()?.id
      val expectedUpdatedRecord = SubjectAccessRequest(
        id = idOfClaimedSarWithPendingStatusAfterThreshold,
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
        claimDateTime = claimDateTimeFormatted,
      )

      var numberOfDbRecordsUpdated = 0
      if (idOfClaimedSarWithPendingStatusAfterThreshold != null) {
        numberOfDbRecordsUpdated = sarRepository?.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
          idOfClaimedSarWithPendingStatusAfterThreshold,
          thresholdClaimDateTimeFormatted,
          currentDateTimeFormatted,
        )!!
      }

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(0)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(idOfClaimedSarWithPendingStatusAfterThreshold))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class updateStatus {
    @Test
    fun `updates status`() {
      databaseInsert()

      val idOfSarWithPendingStatusClaimedEarlier = sarRepository?.findAll()?.last()?.id
      val newStatus = Status.Completed
      val expectedUpdatedRecord = SubjectAccessRequest(
        id = idOfSarWithPendingStatusClaimedEarlier,
        status = newStatus,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTimeFormatted,
        claimAttempts = 1,
        claimDateTime = claimDateTimeEarlierFormatted,
      )

      var numberOfDbRecordsUpdated = 0
      if (idOfSarWithPendingStatusClaimedEarlier != null) {
        numberOfDbRecordsUpdated = sarRepository?.updateStatus(
          idOfSarWithPendingStatusClaimedEarlier,
          newStatus,
        )!!
      }

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(idOfSarWithPendingStatusClaimedEarlier))
        .isEqualTo(expectedUpdatedRecord)
    }
  }
}
