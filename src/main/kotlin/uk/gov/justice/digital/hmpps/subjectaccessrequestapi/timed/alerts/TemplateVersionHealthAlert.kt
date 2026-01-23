package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
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

  private companion object {
    private val LOG = LoggerFactory.getLogger(TemplateVersionHealthAlert::class.java)
  }

  @Scheduled(
    fixedDelayString = "\${application.alerts.template-health.alert-interval-ms:300000}",
    timeUnit = TimeUnit.MILLISECONDS,
    initialDelayString = "\${random.int[100000,150000]}",
  )
  @SchedulerLock(
    name = "templateVersionHealthAlert",
    lockAtMostFor = "\${application.alerts.template-health.lock-max}",
  )
  fun raiseAlerts() {
    templateVersionHealthService.getUnhealthyTemplateVersionsMeetingNotificationCriteria()
      .takeIf { it.isNotEmpty() }?.let { it ->
        LOG.info(
          "identified services with unhealthy template versions: {}",
          it.joinToString { it.serviceConfiguration!!.serviceName },
        )
        slackNotificationService.sendTemplateHealthAlert(unhealthyTemplates = it)
        templateVersionHealthService.updateLastNotified(
          templateVersionHealthStatuses = it,
          lastNotified = Instant.now(clock),
        )
      }
  }
}
