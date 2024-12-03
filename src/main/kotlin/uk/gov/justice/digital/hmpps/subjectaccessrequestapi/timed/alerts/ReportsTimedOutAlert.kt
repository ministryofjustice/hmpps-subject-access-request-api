package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

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

  @Scheduled(
    fixedDelayString = "\${application.alerts.report-timeout.alert-frequency}",
    timeUnit = TimeUnit.MINUTES,
  )
  fun execute() {
    try {
      val expiredReports = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()
      expiredReports.takeIf { it.isNotEmpty() }?.let {
        alertsService.raiseReportsTimedOutAlert(it)
      }
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException(
          "ReportsTimedOutAlert threw unexpected exception", ex,
        ),
      )
    }
  }
}