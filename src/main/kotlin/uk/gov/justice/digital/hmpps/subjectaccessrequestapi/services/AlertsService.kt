package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestBacklogThresholdException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestProcessingOverdueException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestTimeoutException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.OverdueSubjectAccessRequests
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AlertsService(
  val telemetryClient: TelemetryClient,
  val alertConfig: AlertsConfiguration,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dataTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private const val OVERDUE_REPORTS_MESSAGE =
      "Warning: %d 'Pending' requests have exceeded the processing overdue threshold: '%s'"

    private const val BACKLOG_THRESHOLD_EXCEEDED_MESSAGE =
      "Warning: Pending reports backlog threshold exceeded - timestamp: %s, threshold: %d, backlog: %d "

    private const val REQUESTS_TIMED_OUT_MESSAGE =
      "Warning: %d requests updated to status 'Errored' after not completing within the processing threshold: '%s'"
  }

  fun raiseUnexpectedExceptionAlert(exception: Exception, properties: Map<String, String>? = null) {
    log.warn("unexpected error encountered", exception)

    telemetryClient.trackException(
      exception,
      properties,
      null,
    )

    Sentry.captureException(exception)
  }

  fun raiseOverdueReportAlert(overdueReports: List<OverdueSubjectAccessRequests?>) {
    val msg = OVERDUE_REPORTS_MESSAGE.format(overdueReports.size, alertConfig.overdueAlertConfig.thresholdAsString())

    log.warn(msg)

    telemetryClient.trackEvent(
      "ReportsOverdueAlert",
      mapOf(
        "count" to overdueReports.size.toString(),
        "timestamp" to LocalDateTime.now().format(dataTimeFmt),
        "overdueThreshold" to alertConfig.overdueAlertConfig.thresholdAsString(),
      ),
    )

    Sentry.captureException(SubjectAccessRequestProcessingOverdueException(msg))
  }

  fun raiseReportBacklogThresholdAlert(backlogSize: Int) {
    val msg = BACKLOG_THRESHOLD_EXCEEDED_MESSAGE.format(
      dataTimeFmt.format(LocalDateTime.now()),
      alertConfig.backlogAlertConfig.threshold,
      backlogSize,
    )

    log.warn(msg)

    telemetryClient.trackEvent(
      "PendingRequestsBacklogThresholdAlert",
      mapOf(
        "backlog" to backlogSize.toString(),
        "threshold" to alertConfig.backlogAlertConfig.threshold.toString(),
        "timestamp" to LocalDateTime.now().format(dataTimeFmt),
      ),
    )
    Sentry.captureException(SubjectAccessRequestBacklogThresholdException(msg))
  }

  fun raiseReportsTimedOutAlert(timedOutRequests: List<SubjectAccessRequest?>) {
    val msg = REQUESTS_TIMED_OUT_MESSAGE.format(
      timedOutRequests.size,
      alertConfig.requestTimeoutAlertConfig.thresholdAsString(),
    )
    log.warn(msg)

    telemetryClient.trackEvent(
      "RequestsTimeoutAlert",
      mapOf(
        "backlog" to timedOutRequests.toString(),
        "threshold" to alertConfig.requestTimeoutAlertConfig.thresholdAsString(),
        "timestamp" to LocalDateTime.now().format(dataTimeFmt),
      ),
    )
    Sentry.captureException(SubjectAccessRequestTimeoutException(msg, timedOutRequests.map { it?.id.toString() }))
  }
}
