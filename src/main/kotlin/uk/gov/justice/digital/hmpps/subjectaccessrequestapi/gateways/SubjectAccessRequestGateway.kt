package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Component
class SubjectAccessRequestGateway(@Autowired val repo: SubjectAccessRequestRepository) {
  fun getSubjectAccessRequests(unclaimedOnly: Boolean, search: String, pageNumber: Int?, pageSize: Int?, currentTime: LocalDateTime = LocalDateTime.now()): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      return repo.findUnclaimed(claimDateTime = currentTime.minusMinutes(30))
    }

    var pagination = Pageable.unpaged(Sort.by("RequestDateTime").descending())
    if (pageNumber != null && pageSize != null) {
      pagination = PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending())
    }

    return repo.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(caseReferenceSearch = search, nomisSearch = search, ndeliusSearch = search, pagination = pagination).content
  }

  fun saveSubjectAccessRequest(sar: SubjectAccessRequest) {
    if (sar.dateTo == null) {
      sar.dateTo = LocalDate.now()
    }
    repo.save(sar)
  }
  fun updateSubjectAccessRequestClaim(id: UUID, thresholdTime: LocalDateTime, currentTime: LocalDateTime): Int {
    val result = repo.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(id, thresholdTime, currentTime)
    return result
  }

  fun updateSubjectAccessRequestStatusCompleted(id: UUID): Int {
    val result = repo.updateStatus(id, Status.Completed)
    return result
  }

  fun updateLastDownloadedDateTime(id: UUID, downloadTime: LocalDateTime): Int {
    return repo.updateLastDownloaded(id, downloadTime)
  }
}
