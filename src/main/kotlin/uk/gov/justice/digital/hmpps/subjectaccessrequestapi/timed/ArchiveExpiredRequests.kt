package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestArchiveRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime
import java.util.UUID

/**
 * Delete reports that are older than 7 days.
 */
@Component
class ArchiveExpiredRequests(private val service: ArchiveExpiredRequestsService) {

  @Scheduled(
    fixedDelayString = "\${application.remove-reports.frequency}",
    initialDelayString = "\${random.int[600000,\${application.remove-reports.frequency}]}",
  )
  fun removeExpiredReports() {
    try {
      service.removeExpiredDocumentsAndArchiveRequests()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during expired report removal", e.javaClass.simpleName, e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class ArchiveExpiredRequestsService(
  private val documentStorageClient: DocumentStorageClient,
  private val subjectAccessRequestRepository: SubjectAccessRequestRepository,
  private val subjectAccessRequestArchiveRepository: SubjectAccessRequestArchiveRepository,
  private val telemetryClient: TelemetryClient,
  @param:Value("\${application.remove-reports.age : 7}") private val removeReportsOver: Long,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val PAGE_SIZE = 100
  }

  /**
   * Remove expired Subject Access Request files from the Document store and move the SubjectAccessRequest data to an
   * archive table.
   * Records are only moved to the archive table if the document store delete request is successful (files deleted OR
   * files not found). SARs with unsuccessful document store requests will not be moved to the archive
   * table.
   */
  @Transactional
  fun removeExpiredDocumentsAndArchiveRequests() {
    val removeDateTime = LocalDateTime.now().minusDays(removeReportsOver)
    var pageIndex = 0

    generateSequence {
      getExpiredRequestsPage(pageIndex, removeDateTime)
    }.takeWhile {
      it.isNotEmpty()
    }.forEach { batch ->
        log.info("archive legacy requests task: processing batch {}", pageIndex)

        val successfulDeletions = deleteFromDocumentStore(batch)
        archiveExpiredSubjectAccessRequests(successfulDeletions)

        log.info("archive legacy requests task: batch {} completed", pageIndex)
        pageIndex++
      }
    log.info("archive legacy requests task: complete")
  }

  private fun getExpiredRequestsPage(
    pageIndex: Int,
    removeDateTime: LocalDateTime,
  ): List<SubjectAccessRequest> = subjectAccessRequestRepository.findByRequestDateTimeBefore(
    thresholdTime = removeDateTime,
    page = PageRequest.of(pageIndex, PAGE_SIZE),
  )

  private fun deleteFromDocumentStore(
    batch: List<SubjectAccessRequest>,
  ): List<SubjectAccessRequest> = batch.mapNotNull { subjectAccessRequest ->
    when (val status = documentStorageClient.deleteDocument(documentId = subjectAccessRequest.id)) {
      HttpStatus.NO_CONTENT -> subjectAccessRequest.also {
        log.info("archive legacy requests task: delete SAR: {} files from document store successful", it.id)
      }

      HttpStatus.NOT_FOUND -> subjectAccessRequest.also {
        log.info(
          "archive legacy requests task: no SAR: {} files found in document store, no action required",
          it.id,
        )
      }

      else -> {
        log.error("archive legacy requests task: delete SAR {} failed with status {}", subjectAccessRequest.id, status)
        telemetryClient.trackApiEvent(
          name = "DocumentStorageClientError",
          id = subjectAccessRequest.id.toString(),
          "action" to "deleteExpiredRequestData",
          "responseStatus" to (status?.value().toString() ?: "N/A"),
        )
        null
      }
    }
  }

  private fun archiveExpiredSubjectAccessRequests(removedRequest: List<SubjectAccessRequest>) {
    val archivedRequests = removedRequest.mapToArchivedSubjectAccessRequest()
    subjectAccessRequestArchiveRepository.saveAllAndFlush(archivedRequests)
    subjectAccessRequestRepository.deleteAllById(archivedRequests.getUniqueSarIds())
  }

  private fun List<SubjectAccessRequest>.mapToArchivedSubjectAccessRequest(): List<ArchivedSubjectAccessRequest> = this.flatMap { expiredRequest ->
      expiredRequest.services.map {
        ArchivedSubjectAccessRequest(it)
      }
    }

  private fun List<ArchivedSubjectAccessRequest>.getUniqueSarIds(): List<UUID> = this.distinctBy { it.sarId }.map { it.sarId }
}
