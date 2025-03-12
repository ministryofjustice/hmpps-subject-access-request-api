package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.LocationDetail

@Repository
interface LocationDetailsRepository : JpaRepository<LocationDetail, String> {

  fun findAllByDpsIdIn(dpsIds: List<String>): List<LocationDetail>
}
