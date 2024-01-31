package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
class SubjectAccessRequestService(
  @Autowired val sarDbGateway: SubjectAccessRequestGateway) {

  fun getSubjectAccessRequests(unclaimedOnly: Boolean): List<SubjectAccessRequest?> {

    val subjectAccessRequests = sarDbGateway.getSubjectAccessRequests(unclaimedOnly)
    return subjectAccessRequests
  }


}