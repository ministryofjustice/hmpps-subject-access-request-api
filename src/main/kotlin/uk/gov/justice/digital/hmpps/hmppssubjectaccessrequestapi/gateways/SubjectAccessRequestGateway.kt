package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
@Component
class SubjectAccessRequestGateway(@Autowired val repo: SubjectAccessRequestRepository) {
  fun getSubjectAccessRequests(unclaimedOnly: Boolean): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      // val expiredClaimDate = LocalDateTime.(Now.fiveMinutesAgo)
      // repo.findByStatusAndClaimAttemptsOrClaimDateTime(status: "pending", claimAttempts: "0")
      val subjectAccessRequests: List<SubjectAccessRequest?> = repo.findByClaimAttemptsIs(0)
//          status == pending
//          AND
//          claimAttempts == 0
//          OR claimDateTime == before expiredClaimDate
//
      return subjectAccessRequests
    }
    val response = repo.findAll()
    return response
  }
  fun saveSubjectAccessRequest(sar: SubjectAccessRequest) {
    repo.save(sar)
  }
}
