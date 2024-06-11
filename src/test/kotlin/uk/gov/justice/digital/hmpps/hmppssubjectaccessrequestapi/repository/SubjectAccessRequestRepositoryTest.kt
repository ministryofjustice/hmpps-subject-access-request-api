package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.*
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
  private val requestTimeLater = "30/03/2024 00:00"
  private val requestTimeLaterFormatted = LocalDateTime.parse(requestTimeLater, dateTimeFormatter)
  private val claimDateTime = "30/01/2024 00:00"
  private val claimDateTimeFormatted = LocalDateTime.parse(claimDateTime, dateTimeFormatter)
  private val claimDateTimeEarlier = "30/01/2023 00:00"
  private val claimDateTimeEarlierFormatted = LocalDateTime.parse(claimDateTimeEarlier, dateTimeFormatter)

  val unclaimedSar = SubjectAccessRequest(
    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
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
    id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
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
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    status = Status.Completed,
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
    id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
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
  val sarWithSearchableCaseReference = SubjectAccessRequest(
    id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
    status = Status.Completed,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "testForSearch",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTimeFormatted,
    claimAttempts = 1,
    claimDateTime = claimDateTimeEarlierFormatted,
  )
  val sarWithSearchableNdeliusId = SubjectAccessRequest(
    id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
    status = Status.Completed,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "testForSearch",
    requestedBy = "Test",
    requestDateTime = requestTimeLaterFormatted,
    claimAttempts = 1,
    claimDateTime = claimDateTimeEarlierFormatted,
  )

  fun databaseInsert() {
    sarRepository?.save(unclaimedSar)
    sarRepository?.save(claimedSarWithPendingStatus)
    sarRepository?.save(completedSar)
    sarRepository?.save(sarWithPendingStatusClaimedEarlier)
    sarRepository?.save(sarWithSearchableCaseReference)
    sarRepository?.save(sarWithSearchableNdeliusId)
  }

  val allSars = listOf(unclaimedSar, claimedSarWithPendingStatus, completedSar, sarWithPendingStatusClaimedEarlier, sarWithSearchableCaseReference, sarWithSearchableNdeliusId)

  @Nested
  inner class FindByClaimAttemptsIs {
    @Test
    fun `findByClaimAttemptsIs returns only unclaimed SAR entries if called with 0`() {
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar)
      databaseInsert()
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(sarRepository?.findByStatusIsAndClaimAttemptsIs(Status.Pending, 0)).isEqualTo(expectedUnclaimed)
    }

    @Test
    fun `findByClaimAttemptsIs returns only claimed SAR entries if called with 1 or more`() {
      val expectedClaimed: List<SubjectAccessRequest> =
        listOf(claimedSarWithPendingStatus, sarWithPendingStatusClaimedEarlier)
      databaseInsert()
      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(sarRepository?.findByStatusIsAndClaimAttemptsIs(Status.Pending, 1)).isEqualTo(expectedClaimed)
    }
  }

  @Nested
  inner class FindByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore {
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
  inner class findUnclaimedRecords {
    @Test
    fun `returns only SAR entries that are unclaimed`() {
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar, sarWithPendingStatusClaimedEarlier)
      databaseInsert()

      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
      Assertions.assertThat(
        sarRepository?.findUnclaimedRecords(
          Status.Pending,
          0,
          claimDateTimeFormatted,
        ),
      ).isEqualTo(expectedUnclaimed)
    }
  }

  @Nested
  inner class UpdateSubjectAccessRequestIfClaimDateTimeLessThanWithClaimDateTimeIsAndClaimAttemptsIs {
    @Test
    fun `updates claimDateTime and claimAttempts if claimDateTime before threshold`() {
      val thresholdClaimDateTime = "30/06/2023 00:00"
      val thresholdClaimDateTimeFormatted = LocalDateTime.parse(thresholdClaimDateTime, dateTimeFormatter)
      val currentDateTime = "01/02/2024 00:00"
      val currentDateTimeFormatted = LocalDateTime.parse(currentDateTime, dateTimeFormatter)

      databaseInsert()

      val expectedUpdatedRecord = SubjectAccessRequest(
        id = sarWithPendingStatusClaimedEarlier.id,
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

      val numberOfDbRecordsUpdated = sarRepository?.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
        sarWithPendingStatusClaimedEarlier.id,
        thresholdClaimDateTimeFormatted,
        currentDateTimeFormatted,
      )

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(sarWithPendingStatusClaimedEarlier.id))
        .isEqualTo(expectedUpdatedRecord)
    }

    @Test
    fun `does not update claimDateTime or claimAttempts if claimDateTime after threshold`() {
      val thresholdClaimDateTime = "30/06/2023 00:00"
      val thresholdClaimDateTimeFormatted = LocalDateTime.parse(thresholdClaimDateTime, dateTimeFormatter)
      val currentDateTime = "01/02/2024 00:00"
      val currentDateTimeFormatted = LocalDateTime.parse(currentDateTime, dateTimeFormatter)

      databaseInsert()

      val expectedUpdatedRecord = SubjectAccessRequest(
        id = claimedSarWithPendingStatus.id,
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

      val numberOfDbRecordsUpdated = sarRepository?.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
        claimedSarWithPendingStatus.id,
        thresholdClaimDateTimeFormatted,
        currentDateTimeFormatted,
      )

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(0)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(claimedSarWithPendingStatus.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class UpdateStatus {
    @Test
    fun `updates status`() {
      databaseInsert()

      val newStatus = Status.Completed
      val expectedUpdatedRecord = SubjectAccessRequest(
        id = sarWithPendingStatusClaimedEarlier.id,
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

      val numberOfDbRecordsUpdated = sarRepository?.updateStatus(
        sarWithPendingStatusClaimedEarlier.id,
        newStatus,
      )

      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(4)
      Assertions.assertThat(sarRepository?.getReferenceById(sarWithPendingStatusClaimedEarlier.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class GetReports {
    @Test
    fun `gets reports from database`() {
      databaseInsert()
      val page: Page<SubjectAccessRequest> = PageImpl(allSars)

      val dbReports = sarRepository?.findAll(PageRequest.of(0, 6))

      Assertions.assertThat(dbReports?.content).isEqualTo(page.content)
      Assertions.assertThat(dbReports?.size).isEqualTo(6)
    }
  }

  @Nested
  inner class FindAll {
    val sarRequestedFirst = SubjectAccessRequest(
      id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
      status = Status.Completed,
      dateFrom = dateFromFormatted,
      dateTo = dateToFormatted,
      sarCaseReferenceNumber = "1234abc",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "1",
      requestedBy = "Test",
      requestDateTime = LocalDateTime.parse("01/01/2000 00:00", dateTimeFormatter),
      claimAttempts = 1,
      claimDateTime = claimDateTimeFormatted,
    )

    val sarRequestedSecond = SubjectAccessRequest(
      id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
      status = Status.Completed,
      dateFrom = dateFromFormatted,
      dateTo = dateToFormatted,
      sarCaseReferenceNumber = "1234abc",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "1",
      requestedBy = "Test",
      requestDateTime = LocalDateTime.parse("01/01/2010 00:00", dateTimeFormatter),
      claimAttempts = 1,
      claimDateTime = claimDateTimeFormatted,
    )

    val sarRequestedThird = SubjectAccessRequest(
      id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
      status = Status.Completed,
      dateFrom = dateFromFormatted,
      dateTo = dateToFormatted,
      sarCaseReferenceNumber = "1234abc",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "1",
      requestedBy = "Test",
      requestDateTime = LocalDateTime.parse("01/01/2020 00:00", dateTimeFormatter),
      claimAttempts = 1,
      claimDateTime = claimDateTimeFormatted,
    )

    private fun databaseInsert() {
      sarRepository?.save(sarRequestedFirst)
      sarRepository?.save(sarRequestedSecond)
      sarRepository?.save(sarRequestedThird)
    }

    @Test
    fun `findAll returns sorted and paginated responses when given relevant arguments`() {
      databaseInsert()
      val expectedPage: Page<SubjectAccessRequest> = PageImpl(listOf(sarRequestedThird, sarRequestedSecond))

      val firstPageOfReports = sarRepository?.findAll(PageRequest.of(0, 2, Sort.by("RequestDateTime").descending()))

      Assertions.assertThat(firstPageOfReports?.content).isEqualTo(expectedPage.content)
      Assertions.assertThat(firstPageOfReports?.size).isEqualTo(2)
    }
  }

  @Nested
  inner class findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining {
    @Test
    fun `findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining returns only SAR entries where the given string is contained within the entry and paginates`() {
      val expectedSearchResult: List<SubjectAccessRequest> = listOf(sarWithSearchableCaseReference)

      databaseInsert()

      val result = sarRepository?.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining("test", "test", "test", PageRequest.of(1, 1, Sort.by("RequestDateTime").descending()))?.content

      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(result).isEqualTo(expectedSearchResult)
    }

    @Test
    fun `findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining returns all entries containing given string sorted on request date when unpaged`() {
      val expectedSearchResult: List<SubjectAccessRequest> = listOf(sarWithSearchableNdeliusId, sarWithSearchableCaseReference)

      databaseInsert()

      val result = sarRepository?.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining("test", "test", "test", Pageable.unpaged(Sort.by("RequestDateTime").descending()))?.content

      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(result).isEqualTo(expectedSearchResult)
    }

    @Test
    fun `findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining returns all SAR entries when searching on blank strings`() {
      databaseInsert()

      val result = sarRepository?.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining("", "", "", Pageable.unpaged(Sort.by("RequestDateTime").descending()))?.content

      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(result).containsAll(allSars)
    }
  }
}
