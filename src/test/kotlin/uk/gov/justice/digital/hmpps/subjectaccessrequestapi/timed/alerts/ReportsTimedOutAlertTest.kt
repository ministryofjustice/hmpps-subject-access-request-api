package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.AlertsService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService

class ReportsTimedOutAlertTest {

  private val alertsService: AlertsService = mock()
  private val subjectAccessRequestService: SubjectAccessRequestService = mock()

  private lateinit var timeoutAlert: ReportsTimedOutAlert

  @BeforeEach
  fun setup() {

  }

}