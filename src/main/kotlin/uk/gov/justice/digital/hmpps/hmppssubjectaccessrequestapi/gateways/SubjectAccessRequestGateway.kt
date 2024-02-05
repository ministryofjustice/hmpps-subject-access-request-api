package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SubjectAccessRequestGateway(@Autowired val repo: SubjectAccessRequestRepository) {
  fun getSubjectAccessRequests(unclaimedOnly: Boolean, currentTime: LocalDateTime = LocalDateTime.now()): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      val sarsWithNoClaims = repo.findByClaimAttemptsIs(0)
      val sarsWithExpiredClaims = repo.findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, currentTime.minusMinutes(5))
      val completeList = sarsWithNoClaims.plus(sarsWithExpiredClaims)
      return completeList
    }
    val response = repo.findAll()
    return response
  }

  fun saveSubjectAccessRequest(sar: SubjectAccessRequest) {
    repo.save(sar)
  }
  fun updateSubjectAccessRequest(sar: SubjectAccessRequest, thresholdTime: LocalDateTime, currentTime: LocalDateTime = LocalDateTime.now()): Int {
    if (sar.id != null) {
      val result = repo.updateClaimDateTimeIfBeforeThreshold(sar.id, thresholdTime, currentTime)
      return result
    }
    return 0
  }
}
