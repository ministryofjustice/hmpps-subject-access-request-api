package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.PrisonDetail

@Repository
interface PrisonDetailsRepository : JpaRepository<PrisonDetail, String> {
  fun findByPrisonId(caseloadId: String): PrisonDetail
}
