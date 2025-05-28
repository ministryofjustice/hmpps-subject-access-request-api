package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ExtendedSubjectAccessRequestDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequestAdminSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AdminControllerTest {
  private val subjectAccessRequestService: SubjectAccessRequestService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val adminController = AdminController(subjectAccessRequestService, telemetryClient)

  companion object {
    private val SAR_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555")
    private val ADMIN_SUMMARY = SubjectAccessRequestAdminSummary(
      totalCount = 10,
      completedCount = 1,
      erroredCount = 2,
      overdueCount = 3,
      pendingCount = 4,
      filterCount = 5,
      requests = listOf(
        ExtendedSubjectAccessRequestDetail(
          id = SAR_UUID,
          status = "Completed",
          dateFrom = LocalDate.parse("2025-01-01"),
          dateTo = LocalDate.parse("2025-03-01"),
          sarCaseReferenceNumber = "123",
          services = "",
          nomisId = "",
          ndeliusCaseReferenceId = "",
          requestedBy = "user",
          requestDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
          claimDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
          claimAttempts = 0,
          objectUrl = "url",
          lastDownloaded = null,
          appInsightsEventsUrl = "appinsightsurl",
        ),
      ),
    )
  }

  @Nested
  inner class GetSubjectAccessRequests {
    @Test
    fun `getSubjectAccessRequests is called with filter, search and pagination parameters when specified in controller and returns summary`() {
      whenever(
        subjectAccessRequestService.getSubjectAccessRequestAdminSummary(
          completed = false,
          errored = false,
          overdue = false,
          pending = false,
          search = "testSearchString",
          pageNumber = 1,
          pageSize = 1,
        ),
      ).thenReturn(ADMIN_SUMMARY)

      val result: SubjectAccessRequestAdminSummary = adminController.getSubjectAccessRequests(completed = false, errored = false, overdue = false, pending = false, search = "testSearchString", pageNumber = 1, pageSize = 1)

      verify(subjectAccessRequestService).getSubjectAccessRequestAdminSummary(
        completed = false,
        errored = false,
        overdue = false,
        pending = false,
        search = "testSearchString",
        pageNumber = 1,
        pageSize = 1,
      )
      assertThat(result).isEqualTo(ADMIN_SUMMARY)
    }

    @Test
    fun `getSubjectAccessRequests is called with filter options as true, search = '' and no pagination parameters when unspecified in controller`() {
      whenever(
        subjectAccessRequestService.getSubjectAccessRequestAdminSummary(
          completed = true,
          errored = true,
          overdue = true,
          pending = true,
          search = "",
          pageNumber = null,
          pageSize = null,
        ),
      ).thenReturn(ADMIN_SUMMARY)

      val result: SubjectAccessRequestAdminSummary = adminController.getSubjectAccessRequests()

      verify(subjectAccessRequestService).getSubjectAccessRequestAdminSummary(
        completed = true,
        errored = true,
        overdue = true,
        pending = true,
        search = "",
        pageNumber = null,
        pageSize = null,
      )
      assertThat(result).isEqualTo(ADMIN_SUMMARY)
    }
  }

  @Nested
  inner class RestartSubjectAccessRequest {
    @Test
    fun `restartSubjectAccessRequest is called`() {
      val response = adminController.restartSubjectAccessRequest(SAR_UUID)

      verify(subjectAccessRequestService).restartSubjectAccessRequest(SAR_UUID)
      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
  }
}
