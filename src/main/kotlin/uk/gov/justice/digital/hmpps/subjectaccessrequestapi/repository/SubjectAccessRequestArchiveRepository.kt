package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SubjectAccessRequestArchiveRepository : JpaRepository<ArchivedSubjectAccessRequest, UUID> {

  fun findBySarIdAndServiceName(sarId: UUID, serviceName: String): ArchivedSubjectAccessRequest?

  @Modifying(clearAutomatically = true)
  @Query("DELETE ArchivedSubjectAccessRequest request WHERE request.sarRequestDateTime < :deleteThreshold")
  fun deleteBySarRequestDateTimeBefore(@Param("deleteThreshold") deleteThreshold: LocalDateTime)
}
