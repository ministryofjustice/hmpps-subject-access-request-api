package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.json.JSONObject
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication
import org.springframework.security.test.context.support.WithUserDetails
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubjectAccessRequestControllerTest {
  @Test
  @WithUserDetails("customUsername")
  fun `createSubjectAccessRequestPost returns MockId`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")
    val request = "{ dateFrom: '01/12/2023', dateTo: '03/01/2024', sarCaseReferenceNumber: '1234abc', services: '{1,2,4}', nomisId: '1', ndeliusCaseReferenceId: '1' }"

    val expected: String = "MockId"
    val result: String = SubjectAccessRequestController(auditService, sarRepository)
      .createSubjectAccessRequestPost(request, authentication)

    var json = JSONObject(request)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateFrom = json.get("dateFrom").toString()
    val dateFromFormatted = LocalDate.parse(dateFrom, formatter)

    val dateTo = json.get("dateTo").toString()
    val dateToFormatted = LocalDate.parse(dateTo, formatter)

    verify(sarRepository, times(1)).save(
      SubjectAccessRequest(
        id = null,
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "1",
        ndeliusCaseReferenceId = "1",
        requestedBy = authentication.name,
        requestDateTime = LocalDateTime.now(),
      ),
    )
    Assertions.assertThat(result).isEqualTo(expected)
  }
}