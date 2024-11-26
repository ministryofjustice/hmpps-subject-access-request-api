package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@Component
class ReportsOverdueAlert(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val alertsService: AlertsService,
) {

  /**
   * Scheduled task to raise alerts for subject access requests that have exceeded the processing overdue threshold
   * (see: [uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration]) Default is
   * 12 hours
   */
  @Scheduled(
    fixedDelayString = "\${application.alerts.reports-overdue.alert-frequency:720}",
    timeUnit = TimeUnit.MINUTES,
  )
  fun execute() {
    try {
      val overdueReports = subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary()

      overdueReports.takeIf { it.requests.isNotEmpty() }?.let {
        alertsService.raiseOverdueReportAlert(overdueReports.requests)
      }
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException(
          "ReportsOverdueAlert threw unexpected exception",
          ex,
        ),
      )
    }
  }
}
