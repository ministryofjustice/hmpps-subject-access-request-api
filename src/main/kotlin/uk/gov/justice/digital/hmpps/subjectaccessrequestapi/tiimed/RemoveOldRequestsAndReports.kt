package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.tiimed

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime

/**
 * Delete reports that are older than 7 days.
 */
@Component
class RemoveOldRequestsAndReports(private val service: RemoveOldReportRequestsService) {

  @Scheduled(
    fixedDelayString = "\${application.remove-reports.frequency}",
    initialDelayString = "\${random.int[600000,\${aapplication.remove-reports.frequency}]}",
  )
  fun removeExpiredReports() {
    try {
      service.removeOldRequestsAndReports()
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
class RemoveOldReportRequestsService(
  private val documentStorageGateway: DocumentStorageGateway,
  private val repository: SubjectAccessRequestRepository,
  @Value("\${application.remove-reports.age : 7}") private val removeReportsOver: Long,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun removeOldRequestsAndReports() {
    val removeDateTime = LocalDateTime.now().minusDays(removeReportsOver)
    val subjectAccessRequestsToDelete = repository.findByRequestDateTimeBefore(removeDateTime)
    log.info("Request/reports over {} days removal started. Deleting {} with date/time prior to {}  ", removeReportsOver, subjectAccessRequestsToDelete.size, removeDateTime)
    subjectAccessRequestsToDelete.forEach {
      val result = documentStorageGateway.deleteDocument(it!!.id)
      if (result == HttpStatus.NOT_FOUND || result == HttpStatus.NO_CONTENT) {
        repository.deleteById(it.id)
      } else {
        log.error("Document store delete document failed for {} with {}", it.id, result)
      }
    }
  }
}
