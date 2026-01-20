package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@Component
class BacklogThresholdAlert(
  private val subjectAccessRequestService: SubjectAccessRequestService,
  private val alertsService: AlertsService,
  private val alertsConfiguration: AlertsConfiguration,
) {

  /**
   * Scheduled task to raise alert notifications if the backlog of Pending requests exceeds the configured threshold.
   */
  @Scheduled(
    fixedDelayString = "\${application.alerts.backlog-threshold.alert-interval-minutes:120}",
    timeUnit = TimeUnit.MINUTES,
    initialDelay = 1,
  )
  @SchedulerLock(name = "backlogThresholdAlert")
  fun execute() {
    try {
      val backlogSize = subjectAccessRequestService.countPendingSubjectAccessRequests()
      if (backlogSize > alertsConfiguration.backlogAlertConfig.threshold) {
        alertsService.raiseReportBacklogThresholdAlert(backlogSize)
      }
    } catch (ex: Exception) {
      alertsService.raiseUnexpectedExceptionAlert(
        RuntimeException(
          "ReportBacklogThresholdAlert threw unexpected exception",
          ex,
        ),
      )
    }
  }
}
