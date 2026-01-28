package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Configuration
class AlertsConfiguration(
  val overdueAlertConfig: OverdueAlertConfiguration,
  val backlogAlertConfig: BacklogAlertConfiguration,
  val requestTimeoutAlertConfig: RequestTimeoutAlertConfiguration,
)

@Configuration
class OverdueAlertConfiguration(
  @param:Value("\${application.alerts.reports-overdue.threshold:12}") val threshold: Long,
  @param:Value("\${application.alerts.reports-overdue.threshold-unit:HOURS}") val thresholdChronoUnit: ChronoUnit,
  @param:Value("\${application.alerts.reports-overdue.alert-interval-minutes:720}") val alertIntervalMinutes: Int,
) {
  fun calculateOverdueThreshold(): LocalDateTime = LocalDateTime.now().minus(threshold, thresholdChronoUnit)
  fun thresholdAsString() = "$threshold $thresholdChronoUnit"
  fun thresholdAlertFrequency() = "$alertIntervalMinutes ${ChronoUnit.MINUTES}"
}

@Configuration
class BacklogAlertConfiguration(
  @param:Value("\${application.alerts.backlog-threshold.threshold:100}") val threshold: Int,
  @param:Value("\${application.alerts.backlog-threshold.alert-interval-minutes:720}") val alertIntervalMinutes: Int,
) {
  fun thresholdAlertFrequency() = "$alertIntervalMinutes ${ChronoUnit.MINUTES}"
}

@Configuration
class RequestTimeoutAlertConfiguration(
  @param:Value("\${application.alerts.report-timeout.threshold:48}") val threshold: Long,
  @param:Value("\${application.alerts.report-timeout.threshold-unit:HOURS}") val thresholdChronoUnit: ChronoUnit,
  @param:Value("\${application.alerts.report-timeout.alert-interval-minutes:}") val alertIntervalMinutes: Int,
) {
  fun calculateTimeoutThreshold(): LocalDateTime = LocalDateTime.now().minus(threshold, thresholdChronoUnit)
  fun thresholdAsString() = "$threshold $thresholdChronoUnit"
}
