package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@Component
class ReportsOverdueAlert(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val alertsService: AlertsService,
) {

  /**
   * Scheduled task to raise alerts if there are subject access requests with status
   * [uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status.Pending] submitted before Time.now - threshold.
   */
  @Scheduled(
    fixedDelayString = "\${application.alerts.reports-overdue.alert-interval-minutes:180}",
    timeUnit = TimeUnit.MINUTES,
    initialDelay = 1,
  )
  fun execute() {
    Status.Pending
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
