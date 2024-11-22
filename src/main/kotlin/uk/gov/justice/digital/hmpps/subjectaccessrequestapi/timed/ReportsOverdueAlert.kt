package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.ReportsOverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import java.util.concurrent.TimeUnit

@Component
class ReportsOverdueAlert(
  val subjectAccessRequestRepository: SubjectAccessRequestRepository,
  val alertConfiguration: ReportsOverdueAlertConfiguration,
  val alertsService: AlertsService,
) {

  /**
   * Scheduled task to raise alerts for subject access requests that have exceeded the processing overdue threshold
   * (see: [uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.ReportsOverdueAlertConfiguration]) Default is
   * 12 hours
   */
  @Scheduled(
    fixedDelayString = "\${application.alerts.reports-overdue.frequency-minutes:720}",
    timeUnit = TimeUnit.MINUTES,
  )
  fun alertOverdueRequests() {
    val overdueReports = subjectAccessRequestRepository.findOverdueSubjectAccessRequests(
      alertConfiguration.calculateOverdueThreshold(),
    )
    overdueReports.takeIf { it.isNotEmpty() }?.let {
      alertsService.raiseOverdueReportAlert(overdueReports)
    }
  }
}
