package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Component
class SubjectAccessRequestGateway(@Autowired val repo: SubjectAccessRequestRepository) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getSubjectAccessRequests(unclaimedOnly: Boolean, filters: String, currentTime: LocalDateTime = LocalDateTime.now()): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      val sarsWithNoClaims = repo.findByStatusIsAndClaimAttemptsIs(Status.Pending, 0)
      val sarsWithExpiredClaims = repo.findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(Status.Pending, 0, currentTime.minusMinutes(5))
      val fullUnclaimedList = sarsWithNoClaims.plus(sarsWithExpiredClaims)
      return fullUnclaimedList
    }
    if (filters !== "") {
      var filterObject = JSONObject("{}")
      try {
        filterObject = JSONObject(filters)
      } catch (exception: JSONException) {
        log.error("Error parsing filters to JSON object: $filters")
      }
      val caseReferenceFilter = filterObject.optString("sarCaseReferenceNumber", "")
      val subjectIdFilter = filterObject.optString("subjectId", "")
      return repo.findFilteredRecords(caseReferenceFilter, subjectIdFilter)
    }
    return repo.findAll()
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

  fun getAllReports(pageNumber: Int, pageSize: Int): Page<SubjectAccessRequest?> {
    val reports = repo.findAll(PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending()))
    try {
      reports.content
      return reports
    } catch (exception: NullPointerException) {
      return Page.empty()
    }
  }
}
