package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestArchiveRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime

class ArchiveExpiredRequestsServiceTest : ArchiveExpiredRequestsTestFixture() {

  private val documentStorageClient: DocumentStorageClient = mock()
  private val subjectAccessRequestRepository: SubjectAccessRequestRepository = mock()
  private val subjectAccessRequestArchiveRepository: SubjectAccessRequestArchiveRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val pageRequest = PageRequest.of(0, 100)
  private val archiveRequestCaptor = argumentCaptor<List<ArchivedSubjectAccessRequest>>()

  private val archiveExpiredRequestsService = ArchiveExpiredRequestsService(
    documentStorageClient,
    subjectAccessRequestRepository,
    subjectAccessRequestArchiveRepository,
    telemetryClient,
    7L,
  )

  private val verifyThresholdTime: (LocalDateTime) -> Boolean = {
    val now = LocalDateTime.now()
    it.isAfter(now.minusDays(7).minusMinutes(1)) && it.isBefore(now.minusDays(7).plusMinutes(1))
  }

  @Test
  fun `should successfully archive expired reports`() {
    whenever(subjectAccessRequestRepository.findByRequestDateTimeBefore(any(), eq(pageRequest)))
      .thenReturn(listOf(sar1))
      .thenReturn(emptyList())

    whenever(documentStorageClient.deleteDocument(any()))
      .thenReturn(HttpStatus.NO_CONTENT)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findByRequestDateTimeBefore(
        thresholdTime = argThat { verifyThresholdTime(this) },
        page = eq(pageRequest),
      )

    verify(documentStorageClient, times(1))
      .deleteDocument(sar1.id)

    verify(subjectAccessRequestArchiveRepository, times(1))
      .saveAllAndFlush(archiveRequestCaptor.capture())

    assertThat(archiveRequestCaptor.allValues).hasSize(1)
    val actual = archiveRequestCaptor.firstValue

    assertThat(actual).hasSize(3)
    assertIsEqualIgnoringId(actual = actual[0], expected = archivedSAR_1_1)
    assertIsEqualIgnoringId(actual = actual[1], expected = archivedSAR_1_2)
    assertIsEqualIgnoringId(actual = actual[2], expected = archivedSAR_1_3)

    verify(subjectAccessRequestRepository, times(1))
      .deleteAllById(listOf(sar1.id))
  }

  @Test
  fun `should successfully archive expired reports when files not found in document store`() {
    whenever(subjectAccessRequestRepository.findByRequestDateTimeBefore(any(), eq(pageRequest)))
      .thenReturn(listOf(sar1))
      .thenReturn(emptyList())

    whenever(documentStorageClient.deleteDocument(any()))
      .thenReturn(HttpStatus.NOT_FOUND)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findByRequestDateTimeBefore(
        thresholdTime = argThat { verifyThresholdTime(this) },
        page = eq(pageRequest),
      )

    verify(documentStorageClient, times(1))
      .deleteDocument(sar1.id)

    verify(subjectAccessRequestArchiveRepository, times(1))
      .saveAllAndFlush(archiveRequestCaptor.capture())

    assertThat(archiveRequestCaptor.allValues).hasSize(1)
    val actual = archiveRequestCaptor.firstValue

    assertThat(actual).hasSize(3)
    assertIsEqualIgnoringId(actual = actual[0], expected = archivedSAR_1_1)
    assertIsEqualIgnoringId(actual = actual[1], expected = archivedSAR_1_2)
    assertIsEqualIgnoringId(actual = actual[2], expected = archivedSAR_1_3)

    verify(subjectAccessRequestRepository, times(1))
      .deleteAllById(listOf(sar1.id))
  }

  @Test
  fun `should successfully archive expired reports when some files not found in document store`() {
    whenever(subjectAccessRequestRepository.findByRequestDateTimeBefore(any(), eq(pageRequest)))
      .thenReturn(listOf(sar1, sar2))
      .thenReturn(emptyList())

    whenever(documentStorageClient.deleteDocument(sar1.id))
      .thenReturn(HttpStatus.NO_CONTENT)

    whenever(documentStorageClient.deleteDocument(sar2.id))
      .thenReturn(HttpStatus.NOT_FOUND)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findByRequestDateTimeBefore(
        thresholdTime = argThat { verifyThresholdTime(this) },
        page = eq(pageRequest),
      )

    verify(documentStorageClient, times(1))
      .deleteDocument(sar1.id)

    verify(documentStorageClient, times(1))
      .deleteDocument(sar2.id)

    verify(subjectAccessRequestArchiveRepository, times(1))
      .saveAllAndFlush(archiveRequestCaptor.capture())

    assertThat(archiveRequestCaptor.allValues).hasSize(1)
    val actual = archiveRequestCaptor.firstValue
    assertThat(actual).hasSize(4)
    assertIsEqualIgnoringId(actual = actual[0], expected = archivedSAR_1_1)
    assertIsEqualIgnoringId(actual = actual[1], expected = archivedSAR_1_2)
    assertIsEqualIgnoringId(actual = actual[2], expected = archivedSAR_1_3)
    assertIsEqualIgnoringId(actual = actual[3], expected = archivedSAR_2_1)

    verify(subjectAccessRequestRepository, times(1))
      .deleteAllById(listOf(sar1.id, sar2.id))
  }

  @Test
  fun `should only archive reports if there are no errors removing files from document store`() {
    whenever(subjectAccessRequestRepository.findByRequestDateTimeBefore(any(), eq(pageRequest)))
      .thenReturn(listOf(sar1, sar3))
      .thenReturn(emptyList())

    // Request 1 fail with unexpected error
    whenever(documentStorageClient.deleteDocument(sar1.id))
      .thenReturn(HttpStatus.INTERNAL_SERVER_ERROR)

    // Request 2 delete documents successfully
    whenever(documentStorageClient.deleteDocument(sar3.id))
      .thenReturn(HttpStatus.NO_CONTENT)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findByRequestDateTimeBefore(
        thresholdTime = argThat { verifyThresholdTime(this) },
        page = eq(pageRequest),
      )

    verify(documentStorageClient, times(1))
      .deleteDocument(sar1.id)

    verify(documentStorageClient, times(1))
      .deleteDocument(sar3.id)

    verify(subjectAccessRequestArchiveRepository, times(1))
      .saveAllAndFlush(archiveRequestCaptor.capture())

    // Only the successful document store deletion is archived
    assertThat(archiveRequestCaptor.allValues).hasSize(1)

    val actual = archiveRequestCaptor.firstValue
    assertThat(actual).hasSize(1)
    assertIsEqualIgnoringId(actual = actual[0], expected = archivedSAR_3_1)

    verify(subjectAccessRequestRepository, times(1))
      .deleteAllById(listOf(sar3.id))

    verify(telemetryClient, times(1)).trackApiEvent(
      name = "DocumentStorageClientError",
      id = sar1.id.toString(),
      "action" to "deleteExpiredRequestData",
      "responseStatus" to "500",
    )
  }

  fun assertIsEqualIgnoringId(actual: ArchivedSubjectAccessRequest, expected: ArchivedSubjectAccessRequest) {
    assertThat(actual)
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(expected)
  }
}
