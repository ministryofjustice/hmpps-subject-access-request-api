package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.OverdueSubjectAccessRequests
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ReportsOverdueSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ReportsOverdueAlertTest {

  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val alertConfiguration: AlertsConfiguration = mock()
  private val alertsService: AlertsService = mock()
  private val dateTimeNowMinus1Hour = LocalDateTime.now().minusHours(1)
  private val overdueOne: OverdueSubjectAccessRequests = mock()
  private val overdueTwo: OverdueSubjectAccessRequests = mock()
  private val overdueThree: OverdueSubjectAccessRequests = mock()

  @Captor
  private lateinit var exceptionCaptor: ArgumentCaptor<Exception>

  private val reportsOverdueAlert = ReportsOverdueAlert(
    subjectAccessRequestService,
    alertsService,
  )

  @BeforeEach
  fun setup() {
    whenever(alertConfiguration.calculateOverdueThreshold())
      .thenReturn(dateTimeNowMinus1Hour)
  }

  @Test
  fun `should not raise alert when no overdue reports are identified`() {
    whenever(subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary())
      .thenReturn(ReportsOverdueSummary("12 Hours", emptyList()))

    reportsOverdueAlert.execute()

    verify(subjectAccessRequestService, times(1))
      .getOverdueSubjectAccessRequestsSummary()

    verify(alertsService, never())
      .raiseOverdueReportAlert(anyList())
  }

  @Test
  fun `should raise alert when overdue reports are identified`() {
    whenever(subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary())
      .thenReturn(ReportsOverdueSummary("12 Hours", listOf(overdueOne, overdueTwo, overdueThree)))

    reportsOverdueAlert.execute()

    verify(subjectAccessRequestService, times(1))
      .getOverdueSubjectAccessRequestsSummary()

    verify(alertsService, times(1))
      .raiseOverdueReportAlert(eq(listOf(overdueOne, overdueTwo, overdueThree)))
  }

  @Test
  fun `should trigger alert when subjectAccessRequestService throws exception`() {
    val expectedCause = RuntimeException("some error")
    whenever(subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary())
      .thenThrow(expectedCause)

    reportsOverdueAlert.execute()

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(capture(exceptionCaptor), eq(null))
    verify(alertsService, never()).raiseOverdueReportAlert(anyList())

    assertThat(exceptionCaptor.allValues).hasSize(1)
    assertThat(exceptionCaptor.allValues[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(exceptionCaptor.allValues[0].message).isEqualTo("ReportsOverdueAlert threw unexpected exception")
    assertThat(exceptionCaptor.allValues[0].cause).isEqualTo(expectedCause)
  }
}
