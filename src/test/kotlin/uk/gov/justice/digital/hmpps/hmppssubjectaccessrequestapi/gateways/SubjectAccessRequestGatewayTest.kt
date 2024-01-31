package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
class SubjectAccessRequestGatewayTest {
  @Test
  fun `gateway calls findAll if unclaimed is false`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
      .getSubjectAccessRequests(unclaimedOnly = false)
    verify(sarRepository, times(1)).findAll();
  }

  @Test
  fun `gateway calls findByClaimAttemptsIs if unclaimed is true`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val result: List<SubjectAccessRequest?> = SubjectAccessRequestGateway(sarRepository)
      .getSubjectAccessRequests(unclaimedOnly = true)
    verify(sarRepository, times(1)).findByClaimAttemptsIs(0);
  }
}