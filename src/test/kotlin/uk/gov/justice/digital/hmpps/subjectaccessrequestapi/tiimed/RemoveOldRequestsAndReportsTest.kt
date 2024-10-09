package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.tiimed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime
import java.util.UUID

class RemoveOldRequestsAndReportsTest {
  private val documentStorageGateway: DocumentStorageGateway = mock()
  private val repository: SubjectAccessRequestRepository = mock()

  private val removeOldReportRequestsService = RemoveOldReportRequestsService(documentStorageGateway, repository, 7L)

  private val subjectAccessRequestList = listOf(
    SubjectAccessRequest(
      id = UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"),
      status = Status.Completed,
      sarCaseReferenceNumber = "a",
      services = "bob",
      claimAttempts = 5,
    ),
    SubjectAccessRequest(
      id = UUID.fromString("136d9411-21e5-4180-86b0-a561b8752127"),
      status = Status.Pending,
      sarCaseReferenceNumber = "b",
      services = "bob",
      claimAttempts = 5,
    ),
  )

  @Test
  fun removeOldReportRequestsNoContentFromDocumentService() {
    whenever(repository.findByRequestDateTimeBefore(any())).thenReturn(subjectAccessRequestList)
    whenever(documentStorageGateway.deleteDocument(any())).thenReturn(HttpStatus.NO_CONTENT)

    removeOldReportRequestsService.removeOldRequestsAndReports()
    verify(repository).findByRequestDateTimeBefore(
      org.mockito.kotlin.check {
        val now = LocalDateTime.now()
        assertThat(it).isBetween(now.minusDays(7).minusMinutes(1), now.minusDays(7).plusMinutes(1))
      },
    )
    verify(
      documentStorageGateway,
      times(2),
    ).deleteDocument(any())
    verify(repository, times(2)).deleteById(any())
  }

  @Test
  fun removeOldReportRequestsNotFoundFromDocumentService() {
    whenever(repository.findByRequestDateTimeBefore(any())).thenReturn(subjectAccessRequestList)
    whenever(documentStorageGateway.deleteDocument(any())).thenReturn(HttpStatus.NOT_FOUND)

    removeOldReportRequestsService.removeOldRequestsAndReports()
    verify(repository).findByRequestDateTimeBefore(
      org.mockito.kotlin.check {
        val now = LocalDateTime.now()
        assertThat(it).isBetween(now.minusDays(7).minusMinutes(1), now.minusDays(7).plusMinutes(1))
      },
    )
    verify(
      documentStorageGateway,
      times(2),
    ).deleteDocument(any())
    verify(repository, times(2)).deleteById(any())
  }

  @Test
  fun removeOldReportRequestsNoContentNotFoundFromDocumentService() {
    whenever(repository.findByRequestDateTimeBefore(any())).thenReturn(subjectAccessRequestList)
    whenever(documentStorageGateway.deleteDocument(UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"))).thenReturn(HttpStatus.NO_CONTENT)
    whenever(documentStorageGateway.deleteDocument(UUID.fromString("136d9411-21e5-4180-86b0-a561b8752127"))).thenReturn(HttpStatus.NOT_FOUND)

    removeOldReportRequestsService.removeOldRequestsAndReports()
    verify(repository).findByRequestDateTimeBefore(
      org.mockito.kotlin.check {
        val now = LocalDateTime.now()
        assertThat(it).isBetween(now.minusDays(7).minusMinutes(1), now.minusDays(7).plusMinutes(1))
      },
    )
    verify(
      documentStorageGateway,
      times(2),
    ).deleteDocument(any())
    verify(repository, times(2)).deleteById(any())
  }

  @Test
  fun removeOldReportRequestsErrorResponseFromDocumentService() {
    whenever(repository.findByRequestDateTimeBefore(any())).thenReturn(subjectAccessRequestList)
    whenever(documentStorageGateway.deleteDocument(UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"))).thenReturn(HttpStatus.NO_CONTENT)
    whenever(documentStorageGateway.deleteDocument(UUID.fromString("136d9411-21e5-4180-86b0-a561b8752127"))).thenReturn(HttpStatus.BAD_REQUEST)

    removeOldReportRequestsService.removeOldRequestsAndReports()
    verify(repository).findByRequestDateTimeBefore(
      org.mockito.kotlin.check {
        val now = LocalDateTime.now()
        assertThat(it).isBetween(now.minusDays(7).minusMinutes(1), now.minusDays(7).plusMinutes(1))
      },
    )
    verify(
      documentStorageGateway,
      times(2),
    ).deleteDocument(any())
    verify(repository, times(1)).deleteById(UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"))
    verify(repository, times(0)).deleteById(UUID.fromString("136d9411-21e5-4180-86b0-a561b8752127"))
  }
}
