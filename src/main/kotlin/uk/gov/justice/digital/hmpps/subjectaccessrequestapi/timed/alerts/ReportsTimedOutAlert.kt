package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Component
class ReportsTimedOutAlert(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val alertsService: AlertsService
) {

  @Scheduled(
    fixedDelayString = "\${application.alerts.report-timeout.alert-frequency}",
    timeUnit = TimeUnit.MINUTES,
  )
  fun execute() {
//    val expiredReports = subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold()
//    expiredReports.takeIf { it.isNotEmpty() }?.let {
//      alertsService
//    }
  }
}