package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.scheduling.annotation.Scheduled
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.OverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class ReportsOverdueAlertTest {

  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val alertConfiguration: AlertsConfiguration = mock()
  private val overdueAlertConfig: OverdueAlertConfiguration = mock()
  private val alertsService: AlertsService = mock()
  private val slackNotificationService: SlackNotificationService = mock()
  private val oneHourRequest: SubjectAccessRequest = mock()
  private val twelveHourRequest: SubjectAccessRequest = mock()

  @Captor
  private lateinit var exceptionCaptor: org.mockito.ArgumentCaptor<Exception>

  private val reportsOverdueAlert = ReportsOverdueAlert(
    subjectAccessRequestService,
    alertsService,
    alertConfiguration,
    slackNotificationService,
  )

  @Test
  fun `should schedule overdue checks every fifteen minutes by default`() {
    val schedule = ReportsOverdueAlert::class.java.getDeclaredMethod("execute").getAnnotation(Scheduled::class.java)

    assertThat(schedule.fixedDelayString).isEqualTo("\${application.alerts.reports-overdue.alert-interval-minutes:15}")
    assertThat(schedule.timeUnit).isEqualTo(TimeUnit.MINUTES)
  }

  @Test
  fun `should schedule twelve hour overdue checks every two hours across replicas`() {
    val method = ReportsOverdueAlert::class.java.getDeclaredMethod("executeTwelveHourAlert")
    val schedule = method.getAnnotation(Scheduled::class.java)
    val lock = method.getAnnotation(SchedulerLock::class.java)

    assertThat(schedule.fixedDelayString).isEqualTo("\${application.alerts.reports-overdue.twelve-hour-alert-interval-minutes:120}")
    assertThat(schedule.timeUnit).isEqualTo(TimeUnit.MINUTES)
    assertThat(lock.lockAtLeastFor).isEqualTo("\${application.alerts.reports-overdue.twelve-hour-alert-interval-minutes:120}m")
    assertThat(lock.lockAtMostFor).isEqualTo("PT2H5M")
  }

  @Test
  fun `should not raise alerts when no requests are awaiting notifications`() {
    whenever(subjectAccessRequestService.getRequestsAwaitingOneHourOverdueAlert()).thenReturn(emptyList())
    whenever(subjectAccessRequestService.getRequestsAwaitingTwelveHourOverdueAlert()).thenReturn(emptyList())
    whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
    whenever(overdueAlertConfig.firstAlertThresholdAsString()).thenReturn("1 HOURS")
    whenever(overdueAlertConfig.thresholdAsString()).thenReturn("12 HOURS")

    reportsOverdueAlert.execute()
    reportsOverdueAlert.executeTwelveHourAlert()

    verify(subjectAccessRequestService).getRequestsAwaitingOneHourOverdueAlert()
    verify(subjectAccessRequestService).getRequestsAwaitingTwelveHourOverdueAlert()
    verify(slackNotificationService, never()).sendReportsOverdueAlert(any(), any())
    verify(alertsService, never()).raiseUnexpectedExceptionAlert(any(), any())
  }

  @Test
  fun `should send direct Slack alerts for requests at one and twelve hour thresholds`() {
    whenever(subjectAccessRequestService.getRequestsAwaitingOneHourOverdueAlert()).thenReturn(listOf(oneHourRequest))
    whenever(subjectAccessRequestService.getRequestsAwaitingTwelveHourOverdueAlert()).thenReturn(listOf(twelveHourRequest))
    whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
    whenever(overdueAlertConfig.firstAlertThresholdAsString()).thenReturn("1 HOURS")
    whenever(overdueAlertConfig.thresholdAsString()).thenReturn("12 HOURS")
    reportsOverdueAlert.execute()
    reportsOverdueAlert.executeTwelveHourAlert()

    verify(slackNotificationService).sendReportsOverdueAlert(listOf(oneHourRequest), "1 HOURS")
    verify(slackNotificationService).sendReportsOverdueAlert(listOf(twelveHourRequest), "12 HOURS")
  }

  @Test
  fun `should check only the one hour threshold on the frequent schedule`() {
    whenever(subjectAccessRequestService.getRequestsAwaitingOneHourOverdueAlert()).thenReturn(listOf(oneHourRequest))
    whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
    whenever(overdueAlertConfig.firstAlertThresholdAsString()).thenReturn("1 HOURS")

    reportsOverdueAlert.execute()

    verify(slackNotificationService).sendReportsOverdueAlert(listOf(oneHourRequest), "1 HOURS")
    verify(subjectAccessRequestService, never()).getRequestsAwaitingTwelveHourOverdueAlert()
  }

  @Test
  fun `should check only the twelve hour threshold on the two hour schedule`() {
    whenever(subjectAccessRequestService.getRequestsAwaitingTwelveHourOverdueAlert()).thenReturn(listOf(twelveHourRequest))
    whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
    whenever(overdueAlertConfig.thresholdAsString()).thenReturn("12 HOURS")

    reportsOverdueAlert.executeTwelveHourAlert()

    verify(slackNotificationService).sendReportsOverdueAlert(listOf(twelveHourRequest), "12 HOURS")
    verify(subjectAccessRequestService, never()).getRequestsAwaitingOneHourOverdueAlert()
  }

  @Test
  fun `should raise unexpected alert when twelve hour query throws exception`() {
    val cause = RuntimeException("some error")
    whenever(subjectAccessRequestService.getRequestsAwaitingTwelveHourOverdueAlert()).thenThrow(cause)

    reportsOverdueAlert.executeTwelveHourAlert()

    verify(alertsService).raiseUnexpectedExceptionAlert(capture(exceptionCaptor), eq(null))
    verify(slackNotificationService, never()).sendReportsOverdueAlert(any(), any())
    assertThat(exceptionCaptor.value.cause).isEqualTo(cause)
  }

  @Test
  fun `should raise unexpected alert when subject access request service throws exception`() {
    val expectedCause = RuntimeException("some error")
    whenever(subjectAccessRequestService.getRequestsAwaitingOneHourOverdueAlert())
      .thenThrow(expectedCause)

    reportsOverdueAlert.execute()

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(capture(exceptionCaptor), eq(null))
    verify(slackNotificationService, never()).sendReportsOverdueAlert(any(), any())

    assertThat(exceptionCaptor.allValues).hasSize(1)
    assertThat(exceptionCaptor.allValues[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(exceptionCaptor.allValues[0].message).isEqualTo("ReportsOverdueAlert threw unexpected exception")
    assertThat(exceptionCaptor.allValues[0].cause).isEqualTo(expectedCause)
  }
}
