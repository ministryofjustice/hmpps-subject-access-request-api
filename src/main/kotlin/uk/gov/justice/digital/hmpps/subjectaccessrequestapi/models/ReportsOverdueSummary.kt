package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import org.apache.commons.lang3.time.DurationFormatUtils.formatDuration
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

private const val durationFormat = "H'h' m'm'"

data class ReportsOverdueSummary(
  val overdueThreshold: String,
  val requests: List<OverdueSubjectAccessRequests?>,
) {
  val total: Int = requests.size
}

data class OverdueSubjectAccessRequests(
  val id: UUID,
  val sarCaseReferenceNumber: String,
  val submitted: LocalDateTime?,
  val lastClaimed: LocalDateTime?,
  val claimsAttempted: Int,
  val overdueDuration: Duration
) {
  val durationHumanReadable: String = formatDuration(
    Duration.between(submitted, LocalDateTime.now()).toMillis(), durationFormat,
  )
}