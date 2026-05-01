package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestArchiveRepository
import java.time.LocalDateTime

@Component
class DeleteArchivedRequests(
  val deleteArchivedRequestsService: DeleteArchivedRequestsService,
  val telemetryClient: TelemetryClient,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(
    fixedRateString = $$"${application.delete-archives.frequency}",
    initialDelayString = $$"${random.int[300000,${application.delete-archives.frequency}]}",
  )
  fun execute() {
    try {
      deleteArchivedRequestsService.removeExpiredArchiveEntries()
    } catch (e: Exception) {
      // Non-fatal error but have to catch the exception otherwise scheduling will stop
      telemetryClient.trackException(e, mapOf("action" to "deleteExpiredArchivedRequests"), null)
      log.error("error while executing removeExpiredArchiveEntries task", e)
    }
  }
}

@Service
class DeleteArchivedRequestsService(
  private val subjectAccessRequestArchiveRepository: SubjectAccessRequestArchiveRepository,
  @param:Value($$"${application.delete-archives.after: 365}") private val deleteArchivesAfter: Long,
) {

  @Transactional
  fun removeExpiredArchiveEntries() {
    val cutoffDateTime = LocalDateTime.now().minusDays(deleteArchivesAfter)
    subjectAccessRequestArchiveRepository.deleteBySarRequestDateTimeBefore(deleteThreshold = cutoffDateTime)
  }
}
