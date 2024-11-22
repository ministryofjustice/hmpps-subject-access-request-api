package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Configuration
data class ReportsOverdueAlertConfiguration(
  @Value("\${application.alerts.reports-overdue.threshold:12}") val threshold: Long,
  @Value("\${application.alerts.reports-overdue.unit:HOURS}") val chronoUnit: ChronoUnit,
) {
  fun calculateOverdueThreshold(): LocalDateTime = LocalDateTime.now().minus(threshold, chronoUnit)
  fun thresholdAsString(): String = "$threshold $chronoUnit"
}
