package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@Component
class ReportsTimedOutAlert(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val alertsService: AlertsService,
) {

  /**
   * Scheduled task to fail any requests with status == 'Pending' submitted before the configured threshold
   * (default is 48 hours). Requests matching the criteria are considered to have failed. Identified requests are updated
   * with status 'Errored' and alert notification is raise to prompt the team to investigate.
   */
  @Scheduled(
    fixedDelayString = "\${application.alerts.report-timeout.alert-interval-minutes:30}",
    timeUnit = TimeUnit.MINUTES,
    initialDelay = 1,
  )
  @SchedulerLock(name = "reportsTimedOutAlert")
  fun execute() {
    try {
      val expiredReports = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()
      expiredReports.takeIf { it.isNotEmpty() }?.let {
        alertsService.raiseReportsTimedOutAlert(it)
      }
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException(
          "ReportsTimedOutAlert threw unexpected exception",
          ex,
        ),
      )
    }
  }
}
