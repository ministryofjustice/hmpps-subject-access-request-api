package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface TemplateVersionHealthStatusRepository : JpaRepository<TemplateVersionHealthStatus, UUID> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE TemplateVersionHealthStatus templateVersionHealthStatus " +
      "SET templateVersionHealthStatus.status = :newStatus, templateVersionHealthStatus.lastModified = :currentTime " +
      "WHERE templateVersionHealthStatus.serviceConfiguration.id = :serviceConfigurationId AND templateVersionHealthStatus.status != :newStatus",
  )
  fun updateStatusWhenChanged(
    @Param("serviceConfigurationId") serviceConfigurationId: UUID,
    @Param("newStatus") newStatus: HealthStatusType,
    @Param("currentTime") currentTime: LocalDateTime,
  ): Int
}
