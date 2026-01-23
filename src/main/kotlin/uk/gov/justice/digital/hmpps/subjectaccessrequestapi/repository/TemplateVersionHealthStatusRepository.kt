package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant
import java.util.UUID

@Repository
interface TemplateVersionHealthStatusRepository : JpaRepository<TemplateVersionHealthStatus, UUID> {

  fun findByServiceConfigurationId(serviceConfigurationId: UUID): TemplateVersionHealthStatus?

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE TemplateVersionHealthStatus templateVersionHealthStatus " +
      "SET templateVersionHealthStatus.status = :newStatus, templateVersionHealthStatus.lastModified = :currentTime " +
      "WHERE templateVersionHealthStatus.serviceConfiguration.id = :serviceConfigurationId AND templateVersionHealthStatus.status != :newStatus",
  )
  fun updateStatusWhenChanged(
    @Param("serviceConfigurationId") serviceConfigurationId: UUID,
    @Param("newStatus") newStatus: HealthStatusType,
    @Param("currentTime") currentTime: Instant,
  ): Int

  @Query(
    value = "SELECT t FROM TemplateVersionHealthStatus t " +
      "WHERE t.status = 'UNHEALTHY' " +
      "AND t.lastModified < :unhealthyStatusThreshold " +
      "AND ( t.lastNotified IS NULL OR t.lastNotified < :lastNotifiedThreshold )",
  )
  fun findUnhealthyTemplates(
    @Param("unhealthyStatusThreshold") unhealthyStatusThreshold: Instant,
    @Param("lastNotifiedThreshold") lastNotifiedThreshold: Instant,
  ): List<TemplateVersionHealthStatus>
}
