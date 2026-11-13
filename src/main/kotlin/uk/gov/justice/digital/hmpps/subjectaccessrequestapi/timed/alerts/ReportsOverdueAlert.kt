package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@Component
class ReportsOverdueAlert(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val alertsService: AlertsService,
  val alertsConfiguration: AlertsConfiguration,
  val slackNotificationService: SlackNotificationService,
) {

  @Scheduled(
    fixedDelayString = "\${application.alerts.reports-overdue.alert-interval-minutes:15}",
    timeUnit = TimeUnit.MINUTES,
    initialDelay = 1,
  )
  @SchedulerLock(
    name = "reportsOverdueAlert",
    lockAtLeastFor = "\${application.alerts.reports-overdue.alert-interval-minutes:15}m",
    lockAtMostFor = "PT2H5M",
  )
  fun execute() {
    try {
      sendOverdueAlert(
        requests = subjectAccessRequestService.getRequestsAwaitingOneHourOverdueAlert(),
        threshold = alertsConfiguration.overdueAlertConfig.firstAlertThresholdAsString(),
      )
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException("ReportsOverdueAlert threw unexpected exception", ex),
      )
    }
  }

  @Scheduled(
    fixedDelayString = "\${application.alerts.reports-overdue.twelve-hour-alert-interval-minutes:120}",
    timeUnit = TimeUnit.MINUTES,
    initialDelay = 1,
  )
  @SchedulerLock(
    name = "reportsTwelveHourOverdueAlert",
    lockAtLeastFor = "\${application.alerts.reports-overdue.twelve-hour-alert-interval-minutes:120}m",
    lockAtMostFor = "PT2H5M",
  )
  fun executeTwelveHourAlert() {
    try {
      sendOverdueAlert(
        requests = subjectAccessRequestService.getRequestsAwaitingTwelveHourOverdueAlert(),
        threshold = alertsConfiguration.overdueAlertConfig.thresholdAsString(),
      )
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException(
          "ReportsOverdueAlert threw unexpected exception",
          ex,
        ),
      )
    }
  }

  private fun sendOverdueAlert(
    requests: List<SubjectAccessRequest>,
    threshold: String,
  ) {
    if (requests.isEmpty()) return

    slackNotificationService.sendReportsOverdueAlert(requests, threshold)
  }
}
