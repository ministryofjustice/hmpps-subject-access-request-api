package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.Authentication
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
class SubjectAccessRequestControllerTest {
  @Test
  fun `createSubjectAccessRequestPost returns MockId`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)

    val expected: String = "MockId"
    val result: String = SubjectAccessRequestController(auditService)
      .createSubjectAccessRequestPost("", authentication)

    verify(sarRepository, times(1)).save(SubjectAccessRequest())
    Assertions.assertThat(result).isEqualTo(expected)
  }

}