package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.ReportsOverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestProcessingOverdueException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AlertsServiceImpl(
  val telemetryClient: TelemetryClient,
  val alertConfig: ReportsOverdueAlertConfiguration,
) : AlertsService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dataTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private const val OVERDUE_REPORTS_MESSAGE =
      "Warning: %d reports with status 'Pending' have exceeded the processing overdue threshold: '%s'"
  }

  override fun raiseOverdueReportAlert(overdueReports: List<SubjectAccessRequest?>) {
    val msg = OVERDUE_REPORTS_MESSAGE.format(overdueReports.size, alertConfig.thresholdAsString())

    log.warn(msg)

    telemetryClient.trackEvent(
      "ReportsOverdueAlert",
      mapOf(
        "count" to overdueReports.size.toString(),
        "timestamp" to LocalDateTime.now().format(dataTimeFmt),
        "overdueThreshold" to alertConfig.thresholdAsString(),
      ),
    )

    Sentry.captureException(SubjectAccessRequestProcessingOverdueException(msg))
  }
}
