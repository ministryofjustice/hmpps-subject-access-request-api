package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Configuration
class AlertsConfiguration(
  @Value("\${application.alerts.reports-overdue.threshold:12}") val overdueThreshold: Long,
  @Value("\${application.alerts.reports-overdue.threshold-unit:HOURS}") val overdueThresholdChronoUnit: ChronoUnit,
  @Value("\${application.alerts.reports-overdue.alert-frequency:720}") val overdueAlertFrequencyMinutes: Int,
  @Value("\${application.alerts.backlog-threshold.threshold:100}") val backlogThreshold: Int,
  @Value("\${application.alerts.backlog-threshold.alert-frequency:720}") val backlogThresholdAlertFrequency: Int,
) {

  fun calculateOverdueThreshold(): LocalDateTime =
    LocalDateTime.now().minus(overdueThreshold, overdueThresholdChronoUnit)

  fun overdueThresholdAsString() = "$overdueThreshold $overdueThresholdChronoUnit"

  fun overdueThresholdAlertFrequency() = "$overdueAlertFrequencyMinutes ${ChronoUnit.MINUTES}"

  fun backlogThresholdAlertFrequency() = "$backlogThresholdAlertFrequency ${ChronoUnit.MINUTES}"
}
