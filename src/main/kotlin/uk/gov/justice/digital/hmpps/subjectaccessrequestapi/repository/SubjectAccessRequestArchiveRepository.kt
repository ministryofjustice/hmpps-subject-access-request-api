package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import java.util.UUID

@Repository
interface SubjectAccessRequestArchiveRepository : JpaRepository<ArchivedSubjectAccessRequest, UUID> {

  fun findBySarIdAndServiceName(sarId: UUID, serviceName: String): ArchivedSubjectAccessRequest?
}
