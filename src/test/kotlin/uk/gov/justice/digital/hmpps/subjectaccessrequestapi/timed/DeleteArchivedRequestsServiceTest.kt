package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestArchiveRepository
import java.time.LocalDateTime

class DeleteArchivedRequestsServiceTest : ArchiveExpiredRequestsTestFixture() {

  private val subjectAccessRequestArchiveRepository: SubjectAccessRequestArchiveRepository = mock()

  private val deleteAfterDays: Long = 365

  private val service = DeleteArchivedRequestsService(
    subjectAccessRequestArchiveRepository = subjectAccessRequestArchiveRepository,
    deleteArchivesAfter = deleteAfterDays,
  )

  private val deleteAfterDaysCaptor = argumentCaptor<LocalDateTime>()

  @Test
  fun `successfully delete expired archived request`() {
    service.removeExpiredArchiveEntries()

    verify(subjectAccessRequestArchiveRepository, times(1))
      .deleteBySarRequestDateTimeBefore(deleteAfterDaysCaptor.capture())

    assertThat(deleteAfterDaysCaptor.allValues).hasSize(1)
    assertThat(deleteAfterDaysCaptor.firstValue).isEqualToIgnoringNanos(dateTimeNow.minusDays(deleteAfterDays))
  }

  @Test
  fun `should throw exception if delete unsuccessful`() {
    whenever(subjectAccessRequestArchiveRepository.deleteBySarRequestDateTimeBefore(any()))
      .thenThrow(RuntimeException("Whoops!"))

    val actual = assertThrows<RuntimeException> { service.removeExpiredArchiveEntries() }
    assertThat(actual.message).isEqualTo("Whoops!")

    verify(subjectAccessRequestArchiveRepository, times(1))
      .deleteBySarRequestDateTimeBefore(deleteAfterDaysCaptor.capture())

    assertThat(deleteAfterDaysCaptor.allValues).hasSize(1)
    assertThat(deleteAfterDaysCaptor.firstValue).isEqualToIgnoringNanos(dateTimeNow.minusDays(deleteAfterDays))
  }
}
