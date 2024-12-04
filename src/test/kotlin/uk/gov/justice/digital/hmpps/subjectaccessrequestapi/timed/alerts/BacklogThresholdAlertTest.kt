package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.capture
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.BacklogAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService

@ExtendWith(MockitoExtension::class)
class BacklogThresholdAlertTest {

  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val alertsService: AlertsService = mock()
  private val alertsConfiguration: AlertsConfiguration = mock()
  private val backlogAlertConfig: BacklogAlertConfiguration = mock()

  @Captor
  private lateinit var exceptionCaptor: ArgumentCaptor<Exception>

  @Captor
  private lateinit var propertiesCaptor: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var backlogSizeCaptor: ArgumentCaptor<Int>

  private lateinit var backlogThresholdAlert: BacklogThresholdAlert

  @BeforeEach
  fun setup() {
    whenever(alertsConfiguration.backlogAlertConfig).thenReturn(backlogAlertConfig)
    whenever(backlogAlertConfig.threshold).thenReturn(100)

    backlogThresholdAlert = BacklogThresholdAlert(
      subjectAccessRequestService,
      alertsService,
      alertsConfiguration,
    )
  }

  @Test
  fun `should not trigger alert when pending alerts count is 0`() {
    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService)
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
  }

  @Test
  fun `should not trigger alert when backlog size is less than threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(99)

    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService)
  }

  @Test
  fun `should trigger alert when backlog size is greater than threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(101)

    backlogThresholdAlert.execute()

    verify(alertsService, times(1)).raiseReportBacklogThresholdAlert(
      capture(backlogSizeCaptor),
    )

    assertThat(backlogSizeCaptor.allValues).hasSize(1)
    assertThat(backlogSizeCaptor.allValues[0]).isEqualTo(101)
  }

  @Test
  fun `should not trigger alert when backlog size is equal to threshold`() {
    whenever(subjectAccessRequestService.countPendingSubjectAccessRequests())
      .thenReturn(100)

    backlogThresholdAlert.execute()

    verifyNoInteractions(alertsService)
  }
}
