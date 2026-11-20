package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import net.javacrumbs.shedlock.spring.ExtendedLockConfigurationExtractor
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.capture
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.mock.env.MockEnvironment
import org.springframework.scheduling.annotation.Scheduled
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.BacklogAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class BacklogThresholdAlertTest {

  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val alertsService: AlertsService = mock()
  private val slackNotificationService: SlackNotificationService = mock()
  private val alertsConfiguration: AlertsConfiguration = mock()
  private val backlogAlertConfig: BacklogAlertConfiguration = mock()

  @Captor
  private lateinit var exceptionCaptor: ArgumentCaptor<Exception>

  @Captor
  private lateinit var propertiesCaptor: ArgumentCaptor<Map<String, String>>

  private lateinit var backlogThresholdAlert: BacklogThresholdAlert

  @BeforeEach
  fun setup() {
    whenever(alertsConfiguration.backlogAlertConfig).thenReturn(backlogAlertConfig)
    whenever(backlogAlertConfig.threshold).thenReturn(100)

    backlogThresholdAlert = BacklogThresholdAlert(
      subjectAccessRequestService,
      alertsService,
      alertsConfiguration,
      slackNotificationService,
    )
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = [180])
  fun `should resolve lock durations from the backlog schedule interval`(intervalOverride: Int?) {
    val method = BacklogThresholdAlert::class.java.getDeclaredMethod("execute")
    val schedule = method.getAnnotation(Scheduled::class.java)
    val expectedInterval = (intervalOverride ?: 120).toLong()

    AnnotationConfigApplicationContext().use { context ->
      context.environment = MockEnvironment().apply {
        intervalOverride?.let { setProperty("application.alerts.backlog-threshold.alert-interval-minutes", it.toString()) }
      }
      context.register(ShedLockTestConfiguration::class.java)
      context.refresh()

      val lock = context.getBean(ExtendedLockConfigurationExtractor::class.java)
        .getLockConfiguration(backlogThresholdAlert, method, emptyArray())
        .orElseThrow()

      assertThat(schedule.timeUnit).isEqualTo(TimeUnit.MINUTES)
      assertThat(context.environment.resolveRequiredPlaceholders(schedule.fixedDelayString)).isEqualTo(expectedInterval.toString())
      assertThat(lock.lockAtLeastFor).isEqualTo(Duration.ofMinutes(expectedInterval))
      assertThat(lock.lockAtMostFor).isEqualTo(Duration.ofMinutes(expectedInterval + 5))
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableSchedulerLock(defaultLockAtMostFor = "PT60S")
  class ShedLockTestConfiguration

  @Test
  fun `should not trigger alert when pending alerts count is 0`() {
    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService, slackNotificationService)
  }

  @Test
  fun `should trigger alert when subjectAccessRequestService throws exception`() {
    val thrownException = RuntimeException("some error")

    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenThrow(thrownException)

    backlogThresholdAlert.execute()

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(
      capture(exceptionCaptor),
      capture(propertiesCaptor),
    )

    assertThat(exceptionCaptor.allValues).hasSize(1)
    assertThat(exceptionCaptor.allValues[0].message).isEqualTo("ReportBacklogThresholdAlert threw unexpected exception")
    assertThat(exceptionCaptor.allValues[0].cause).isEqualTo(thrownException)
    assertThat(propertiesCaptor.allValues[0]).isNull()
    verifyNoInteractions(slackNotificationService)
  }

  @Test
  fun `should not trigger alert when backlog size is less than threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(99)

    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService, slackNotificationService)
  }

  @Test
  fun `should only send a Slack alert when backlog size is greater than threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(101)

    backlogThresholdAlert.execute()

    verify(slackNotificationService).sendBacklogThresholdAlert(101, 100)
    verifyNoInteractions(alertsService)
  }

  @Test
  fun `should not trigger alert when backlog size is equal to threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(100)

    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService, slackNotificationService)
  }
}
