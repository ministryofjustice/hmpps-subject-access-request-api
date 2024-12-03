package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService

@ExtendWith(MockitoExtension::class)
class ReportsTimedOutAlertTest {

  private val alertsService: AlertsService = mock()
  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val expiredSar1: SubjectAccessRequest = mock()
  private val thrownException = RuntimeException("KABOOOM!")

  private val timeoutAlert = ReportsTimedOutAlert(
    subjectAccessRequestService = subjectAccessRequestService,
    alertsService = alertsService,
  )

  @Captor
  private lateinit var errorCaptor: ArgumentCaptor<Exception>

  @Captor
  private lateinit var propertiesCaptor: ArgumentCaptor<Map<String, String>>

  @Test
  fun `should not raise alert if no requests were expired`() {
    whenever(subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold())
      .thenReturn(emptyList())

    timeoutAlert.execute()

    verify(subjectAccessRequestService, times(1))
      .expirePendingRequestsSubmittedBeforeThreshold()

    verifyNoInteractions(alertsService)
  }

  @Test
  fun `should raise alert if 1 or more requests were expired`() {
    val timedOutRequests = listOf(expiredSar1)

    whenever(subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold())
      .thenReturn(timedOutRequests)

    timeoutAlert.execute()

    verify(subjectAccessRequestService, times(1))
      .expirePendingRequestsSubmittedBeforeThreshold()

    verify(alertsService, times(1)).raiseReportsTimedOutAlert(timedOutRequests)
  }

  @Test
  fun `should raise unexpected error alert subjectAccessRequestService throws exception`() {
    whenever(subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold())
      .thenThrow(thrownException)

    timeoutAlert.execute()

    verify(subjectAccessRequestService, times(1)).expirePendingRequestsSubmittedBeforeThreshold()

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(
      capture(errorCaptor),
      capture(propertiesCaptor),
    )

    assertThat(errorCaptor.allValues).hasSize(1)
    assertThat(errorCaptor.allValues[0].cause).isEqualTo(thrownException)
    assertThat(errorCaptor.allValues[0].message).isEqualTo("ReportsTimedOutAlert threw unexpected exception")
    assertThat(propertiesCaptor.allValues[0]).isNull()
  }

  @Test
  fun `should raise unexpected error alert alertsService throws exception`() {
    whenever(subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold())
      .thenReturn(listOf(expiredSar1))

    whenever(alertsService.raiseReportsTimedOutAlert(listOf(expiredSar1)))
      .thenThrow(thrownException)

    timeoutAlert.execute()

    verify(subjectAccessRequestService, times(1)).expirePendingRequestsSubmittedBeforeThreshold()

    verify(alertsService, times(1)).raiseReportsTimedOutAlert(listOf(expiredSar1))

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(
      capture(errorCaptor),
      capture(propertiesCaptor),
    )

    assertThat(errorCaptor.allValues).hasSize(1)
    assertThat(errorCaptor.allValues[0].cause).isEqualTo(thrownException)
    assertThat(errorCaptor.allValues[0].message).isEqualTo("ReportsTimedOutAlert threw unexpected exception")
    assertThat(propertiesCaptor.allValues[0]).isNull()
  }

  @Test
  fun `should throw exception if the alerts service raise unexpected error throws exception`() {
    whenever(subjectAccessRequestService.expirePendingRequestsSubmittedBeforeThreshold())
      .thenReturn(listOf(expiredSar1))

    whenever(alertsService.raiseReportsTimedOutAlert(listOf(expiredSar1)))
      .thenThrow(thrownException)

    whenever(alertsService.raiseUnexpectedExceptionAlert(any(), isNull()))
      .thenThrow(thrownException)

    val exception = assertThrows<RuntimeException> { timeoutAlert.execute() }

    assertThat(exception).isEqualTo(thrownException)

    verify(subjectAccessRequestService, times(1)).expirePendingRequestsSubmittedBeforeThreshold()

    verify(alertsService, times(1)).raiseReportsTimedOutAlert(listOf(expiredSar1))

    verify(alertsService, times(1)).raiseUnexpectedExceptionAlert(
      capture(errorCaptor),
      capture(propertiesCaptor),
    )

    assertThat(errorCaptor.allValues).hasSize(1)
    assertThat(errorCaptor.allValues[0].cause).isEqualTo(thrownException)
    assertThat(errorCaptor.allValues[0].message).isEqualTo("ReportsTimedOutAlert threw unexpected exception")
    assertThat(propertiesCaptor.allValues[0]).isNull()
  }
}
