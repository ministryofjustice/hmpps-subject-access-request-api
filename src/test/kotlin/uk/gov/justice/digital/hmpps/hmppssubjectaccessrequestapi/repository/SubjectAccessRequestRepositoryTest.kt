package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
  private val downloadDateTime = "01/06/2024 00:00"
  private val downloadDateTimeFormatted = LocalDateTime.parse(downloadDateTime, dateTimeFormatter)

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
    lastDownloaded = downloadDateTimeFormatted,
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
  inner class FindUnclaimed {
    @Test
    fun `returns only SAR entries that are pending and unclaimed or claimed before the given claimDateTime`() {
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar, sarWithPendingStatusClaimedEarlier)
      databaseInsert()

      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
      Assertions.assertThat(
        sarRepository?.findUnclaimed(
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
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
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
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
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
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
      Assertions.assertThat(sarRepository?.getReferenceById(sarWithPendingStatusClaimedEarlier.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class FindBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining {
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

      val result = sarRepository?.findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(
        "",
        "",
        "",
        Pageable.unpaged(
          Sort.by("RequestDateTime").descending(),
        ),
      )?.content

      Assertions.assertThat(sarRepository?.findAll()).isEqualTo(allSars)
      Assertions.assertThat(result).containsAll(allSars)
    }
  }

  @Nested
  inner class UpdateLastDownloaded {

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
      lastDownloaded = downloadDateTimeFormatted,
    )

    @Test
    fun `updates lastDownloaded`() {
      databaseInsert()
      val newDownloadDateTime = "02/06/2024 00:00"
      val newDownloadDateTimeFormatted = LocalDateTime.parse(newDownloadDateTime, dateTimeFormatter)
      val expectedUpdatedRecord = SubjectAccessRequest(
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
        lastDownloaded = newDownloadDateTimeFormatted,
      )

      val numberOfDbRecordsUpdated = sarRepository?.updateLastDownloaded(
        completedSar.id,
        newDownloadDateTimeFormatted,
      )

      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(6)
      Assertions.assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      Assertions.assertThat(sarRepository?.getReferenceById(completedSar.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class DeleteOldSubjectAccessRequests {

    @Test
    fun `deletes old subjectAccessRequests`() {
      databaseInsert()
      val thresholdTime = "30/02/2024 00:00"
      val thresholdTimeFormatted = LocalDateTime.parse(thresholdTime, dateTimeFormatter)
      sarRepository?.findByRequestDateTimeBefore(thresholdTimeFormatted)
      Assertions.assertThat(sarRepository?.findAll()?.size).isEqualTo(1)
      Assertions.assertThat(sarRepository?.findAll()?.contains(sarWithSearchableNdeliusId))
    }
  }
}
