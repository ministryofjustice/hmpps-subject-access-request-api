package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.TemplateVersionHealthService
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class TemplateVersionHealthAlert(
  val templateVersionHealthService: TemplateVersionHealthService,
  val slackNotificationService: SlackNotificationService,
  val clock: Clock,
) {

  @Scheduled(
    fixedDelayString = "\${application.alerts.template-health-alert.alert-interval-minutes:30}",
    timeUnit = TimeUnit.MINUTES,
    initialDelayString = "\${random.int[60000,90000]}",
  )
  @SchedulerLock(
    name = "templateVersionHealthAlert",
    lockAtMostFor = "\${application.alerts.template-health-alert.lock-max}",
  )
  fun raiseAlerts() {
    templateVersionHealthService.getUnhealthyTemplateVersionsMeetingNotificationCriteria()
      .takeIf { it.isNotEmpty() }?.let {
        slackNotificationService.sendTemplateHealthAlert(unhealthyTemplates = it)
        templateVersionHealthService.updateLastNotified(
          templateVersionHealthStatuses = it,
          lastNotified = Instant.now(clock),
        )
      }
  }
}
