package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import org.springframework.security.test.context.support.WithUserDetails
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
class SubjectAccessRequestControllerTest {
  @Test
  @WithUserDetails("customUsername")
  fun `createSubjectAccessRequestPost returns MockId`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.getName()).thenReturn("aName")

    val expected: String = "MockId"
    val result: String = SubjectAccessRequestController(auditService, sarRepository)
      .createSubjectAccessRequestPost("{ dateFrom: '01/12/2023', dateTo: '03/01/2024', sarCaseReferenceNumber: '1234abc', services: '{1,2,4}', nomisId: '1', ndeliusCaseReferenceId: '1' }", authentication)

    verify(sarRepository, times(1)).save(SubjectAccessRequest())
    Assertions.assertThat(result).isEqualTo(expected)
  }
}