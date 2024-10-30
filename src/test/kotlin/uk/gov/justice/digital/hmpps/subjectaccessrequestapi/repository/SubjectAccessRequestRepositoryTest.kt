package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@DataJpaTest
class SubjectAccessRequestRepositoryTest {
  @Autowired
  lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
  private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)
  private val requestTime = LocalDateTime.parse("30/01/2024 00:00", dateTimeFormatter)
  private val requestTimeLater = LocalDateTime.parse("30/03/2024 00:00", dateTimeFormatter)
  private val claimDateTime = LocalDateTime.parse("30/01/2024 00:00", dateTimeFormatter)
  private val claimDateTimeEarlier = LocalDateTime.parse("30/01/2023 00:00", dateTimeFormatter)
  private val downloadDateTime = LocalDateTime.parse("01/06/2024 00:00", dateTimeFormatter)

  final val unclaimedSar = SubjectAccessRequest(
    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    status = Status.Pending,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 0,
  )
  final val claimedSarWithPendingStatus = SubjectAccessRequest(
    id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    status = Status.Pending,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 1,
    claimDateTime = claimDateTime,
  )
  final val completedSar = SubjectAccessRequest(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    status = Status.Completed,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 1,
    claimDateTime = claimDateTime,
    lastDownloaded = downloadDateTime,
  )
  final val sarWithPendingStatusClaimedEarlier = SubjectAccessRequest(
    id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    status = Status.Pending,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 1,
    claimDateTime = claimDateTimeEarlier,
  )
  final val sarWithSearchableCaseReference = SubjectAccessRequest(
    id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
    status = Status.Completed,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "testForSearch",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 1,
    claimDateTime = claimDateTimeEarlier,
  )
  final val sarWithSearchableNdeliusId = SubjectAccessRequest(
    id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
    status = Status.Completed,
    dateFrom = dateFrom,
    dateTo = dateTo,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "testForSearch",
    requestedBy = "Test",
    requestDateTime = requestTimeLater,
    claimAttempts = 1,
    claimDateTime = claimDateTimeEarlier,
  )

  fun databaseInsert() {
    subjectAccessRequestRepository.save(unclaimedSar)
    subjectAccessRequestRepository.save(claimedSarWithPendingStatus)
    subjectAccessRequestRepository.save(completedSar)
    subjectAccessRequestRepository.save(sarWithPendingStatusClaimedEarlier)
    subjectAccessRequestRepository.save(sarWithSearchableCaseReference)
    subjectAccessRequestRepository.save(sarWithSearchableNdeliusId)
  }

  val allSars = listOf(unclaimedSar, claimedSarWithPendingStatus, completedSar, sarWithPendingStatusClaimedEarlier, sarWithSearchableCaseReference, sarWithSearchableNdeliusId)

  @Nested
  inner class FindUnclaimed {
    @Test
    fun `returns only SAR entries that are pending and unclaimed or claimed before the given claimDateTime`() {
      val expectedUnclaimed: List<SubjectAccessRequest> = listOf(unclaimedSar, sarWithPendingStatusClaimedEarlier)
      databaseInsert()

      assertThat(subjectAccessRequestRepository.findAll().size).isEqualTo(6)
      assertThat(
        subjectAccessRequestRepository.findUnclaimed(
          claimDateTime,
        ),
      ).isEqualTo(expectedUnclaimed)
    }
  }

  @Nested
  inner class UpdateSubjectAccessRequestIfClaimDateTimeLessThanWithClaimDateTimeIsAndClaimAttemptsIs {
    @Test
    fun `updates claimDateTime and claimAttempts if claimDateTime before threshold`() {
      val thresholdClaimDateTime = LocalDateTime.parse("30/06/2023 00:00", dateTimeFormatter)
      val currentDateTime = LocalDateTime.parse("01/02/2024 00:00", dateTimeFormatter)

      databaseInsert()

      val expectedUpdatedRecord = SubjectAccessRequest(
        id = sarWithPendingStatusClaimedEarlier.id,
        status = Status.Pending,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTime,
        claimAttempts = 2,
        claimDateTime = currentDateTime,
      )

      val numberOfDbRecordsUpdated = subjectAccessRequestRepository.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
        sarWithPendingStatusClaimedEarlier.id,
        thresholdClaimDateTime,
        currentDateTime,
      )

      assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      assertThat(subjectAccessRequestRepository.findAll().size).isEqualTo(6)
      assertThat(subjectAccessRequestRepository.getReferenceById(sarWithPendingStatusClaimedEarlier.id))
        .isEqualTo(expectedUpdatedRecord)
    }

    @Test
    fun `does not update claimDateTime or claimAttempts if claimDateTime after threshold`() {
      val thresholdClaimDateTime = LocalDateTime.parse("30/06/2023 00:00", dateTimeFormatter)
      val currentDateTime = LocalDateTime.parse("01/02/2024 00:00", dateTimeFormatter)

      databaseInsert()

      val expectedUpdatedRecord = SubjectAccessRequest(
        id = claimedSarWithPendingStatus.id,
        status = Status.Pending,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTime,
        claimAttempts = 1,
        claimDateTime = claimDateTime,
      )

      val numberOfDbRecordsUpdated = subjectAccessRequestRepository.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
        claimedSarWithPendingStatus.id,
        thresholdClaimDateTime,
        currentDateTime,
      )

      assertThat(numberOfDbRecordsUpdated).isEqualTo(0)
      assertThat(subjectAccessRequestRepository.findAll().size).isEqualTo(6)
      assertThat(subjectAccessRequestRepository.getReferenceById(claimedSarWithPendingStatus.id))
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
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTime,
        claimAttempts = 1,
        claimDateTime = claimDateTimeEarlier,
      )

      val numberOfDbRecordsUpdated = subjectAccessRequestRepository.updateStatus(
        sarWithPendingStatusClaimedEarlier.id,
        newStatus,
      )

      assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      assertThat(subjectAccessRequestRepository.findAll().size).isEqualTo(6)
      assertThat(subjectAccessRequestRepository.getReferenceById(sarWithPendingStatusClaimedEarlier.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class FindBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining {
    @Test
    fun `findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase returns only SAR entries where the given string is contained within the entry and paginates`() {
      val expectedSearchResult: List<SubjectAccessRequest> = listOf(sarWithSearchableCaseReference)

      databaseInsert()

      val result = subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase("test", "test", "test", PageRequest.of(1, 1, Sort.by("RequestDateTime").descending()))?.content

      assertThat(subjectAccessRequestRepository.findAll()).isEqualTo(allSars)
      assertThat(result).isEqualTo(expectedSearchResult)
    }

    @Test
    fun `findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase returns all entries containing given string sorted on request date when unpaged`() {
      val expectedSearchResult: List<SubjectAccessRequest> = listOf(sarWithSearchableNdeliusId, sarWithSearchableCaseReference)

      databaseInsert()

      val result = subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase("test", "test", "test", Pageable.unpaged(Sort.by("RequestDateTime").descending()))?.content

      assertThat(subjectAccessRequestRepository.findAll()).isEqualTo(allSars)
      assertThat(result).isEqualTo(expectedSearchResult)
    }

    @Test
    fun `findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase returns all SAR entries when searching on blank strings`() {
      databaseInsert()

      val result = subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
        "",
        "",
        "",
        Pageable.unpaged(
          Sort.by("RequestDateTime").descending(),
        ),
      ).content

      assertThat(subjectAccessRequestRepository.findAll()).isEqualTo(allSars)
      assertThat(result).containsAll(allSars)
    }

    @Test
    fun `findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase is case insensitive`() {
      val expectedSearchResult: List<SubjectAccessRequest> = listOf(sarWithSearchableNdeliusId, sarWithSearchableCaseReference)

      databaseInsert()

      val result = subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase("TEST", "TEST", "TEST", Pageable.unpaged(Sort.by("RequestDateTime").descending()))?.content

      assertThat(subjectAccessRequestRepository.findAll()).isEqualTo(allSars)
      assertThat(result).isEqualTo(expectedSearchResult)
    }
  }

  @Nested
  inner class UpdateLastDownloaded {

    val completedSar = SubjectAccessRequest(
      id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
      status = Status.Completed,
      dateFrom = dateFrom,
      dateTo = dateTo,
      sarCaseReferenceNumber = "1234abc",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "1",
      requestedBy = "Test",
      requestDateTime = requestTime,
      claimAttempts = 1,
      claimDateTime = claimDateTime,
      lastDownloaded = downloadDateTime,
    )

    @Test
    fun `updates lastDownloaded`() {
      databaseInsert()
      val newDownloadDateTime = LocalDateTime.parse("02/06/2024 00:00", dateTimeFormatter)
      val expectedUpdatedRecord = SubjectAccessRequest(
        id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
        status = Status.Completed,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = "Test",
        requestDateTime = requestTime,
        claimAttempts = 1,
        claimDateTime = claimDateTime,
        lastDownloaded = newDownloadDateTime,
      )

      val numberOfDbRecordsUpdated = subjectAccessRequestRepository.updateLastDownloaded(
        completedSar.id,
        newDownloadDateTime,
      )

      assertThat(subjectAccessRequestRepository.findAll().size).isEqualTo(6)
      assertThat(numberOfDbRecordsUpdated).isEqualTo(1)
      assertThat(subjectAccessRequestRepository.getReferenceById(completedSar.id))
        .isEqualTo(expectedUpdatedRecord)
    }
  }

  @Nested
  inner class FindByRequestDateTimeBefore {

    @Test
    fun `finds old subjectAccessRequests`() {
      databaseInsert()
      val thresholdTime = LocalDateTime.parse("30/02/2024 00:00", dateTimeFormatter)
      val oldSars = subjectAccessRequestRepository.findByRequestDateTimeBefore(thresholdTime)
      assertThat(oldSars.size).isEqualTo(5)
    }
  }
}