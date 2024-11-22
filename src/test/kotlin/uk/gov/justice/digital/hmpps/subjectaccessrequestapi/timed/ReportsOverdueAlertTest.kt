package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.ReportsOverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import java.time.LocalDateTime

class ReportsOverdueAlertTest {

  private val subjectAccessRequestRepository: SubjectAccessRequestRepository = mock()
  private val alertConfiguration: ReportsOverdueAlertConfiguration = mock()
  private val alertsService: AlertsService = mock()
  private val dateTimeNowMinus1Hour = LocalDateTime.now().minusHours(1)
  private val sarRequestOne: SubjectAccessRequest = mock()
  private val sarRequestTwo: SubjectAccessRequest = mock()
  private val sarRequestThree: SubjectAccessRequest = mock()

  private val reportsOverdueAlert = ReportsOverdueAlert(
    subjectAccessRequestRepository,
    alertConfiguration,
    alertsService,
  )

  @BeforeEach
  fun setup() {
    whenever(alertConfiguration.calculateOverdueThreshold())
      .thenReturn(dateTimeNowMinus1Hour)
  }

  @Test
  fun `should not raise alert when no overdue reports are identified`() {
    whenever(subjectAccessRequestRepository.findOverdueSubjectAccessRequests(dateTimeNowMinus1Hour))
      .thenReturn(emptyList())

    reportsOverdueAlert.alertOverdueRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findOverdueSubjectAccessRequests(dateTimeNowMinus1Hour)

    verify(alertConfiguration, times(1))
      .calculateOverdueThreshold()

    verify(alertsService, never())
      .raiseOverdueReportAlert(anyList())
  }

  @Test
  fun `should raise alert when overdue reports are identified`() {
    whenever(subjectAccessRequestRepository.findOverdueSubjectAccessRequests(dateTimeNowMinus1Hour))
      .thenReturn(listOf(sarRequestOne, sarRequestTwo, sarRequestThree))

    reportsOverdueAlert.alertOverdueRequests()

    verify(subjectAccessRequestRepository, times(1))
      .findOverdueSubjectAccessRequests(dateTimeNowMinus1Hour)

    verify(alertConfiguration, times(1))
      .calculateOverdueThreshold()

    verify(alertsService, times(1))
      .raiseOverdueReportAlert(eq(listOf(sarRequestOne, sarRequestTwo, sarRequestThree)))
  }
}
