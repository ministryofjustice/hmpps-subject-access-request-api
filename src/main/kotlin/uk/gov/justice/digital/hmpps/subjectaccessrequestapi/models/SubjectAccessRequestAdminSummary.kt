package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import org.apache.commons.lang3.time.DurationFormatUtils.formatDuration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val DURATION_FORMAT = "H'h' m'm'"

data class SubjectAccessRequestAdminSummary(
  val totalCount: Long,
  val completedCount: Int,
  val pendingCount: Int,
  val overdueCount: Int,
  val erroredCount: Int,
  val filterCount: Long,
  val requests: List<ExtendedSubjectAccessRequestDetail>,
)

data class ExtendedSubjectAccessRequestDetail(
  val id: UUID,
  val status: String,
  val dateFrom: LocalDate?,
  var dateTo: LocalDate?,
  val sarCaseReferenceNumber: String,
  val services: String,
  val nomisId: String?,
  val ndeliusCaseReferenceId: String?,
  val requestedBy: String,
  val requestDateTime: LocalDateTime,
  val claimDateTime: LocalDateTime?,
  val claimAttempts: Int,
  val objectUrl: String?,
  val lastDownloaded: LocalDateTime?,
) {
  val durationHumanReadable: String = formatDuration(
    Duration.between(requestDateTime, LocalDateTime.now()).toMillis(),
    DURATION_FORMAT,
  )
}
