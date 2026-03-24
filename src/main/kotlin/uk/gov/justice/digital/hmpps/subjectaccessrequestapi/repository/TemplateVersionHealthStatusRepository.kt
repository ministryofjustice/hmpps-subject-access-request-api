package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant
import java.util.UUID

@Repository
interface TemplateVersionHealthStatusRepository : JpaRepository<TemplateVersionHealthStatus, UUID> {

  fun findByServiceConfigurationId(serviceConfigurationId: UUID): TemplateVersionHealthStatus?

  @Query(
    value = "SELECT t FROM TemplateVersionHealthStatus t " +
      "WHERE t.serviceConfiguration.id IN (:serviceConfigurationIds)",
  )
  fun findByServiceConfigurationIds(
    @Param("serviceConfigurationIds") serviceConfigurationIds: List<UUID>,
  ): List<TemplateVersionHealthStatus>

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE TemplateVersionHealthStatus templateVersionHealthStatus " +
      "SET templateVersionHealthStatus.status = 'HEALTHY', templateVersionHealthStatus.lastModified = :currentTime, templateVersionHealthStatus.lastNotified = NULL " +
      "WHERE templateVersionHealthStatus.serviceConfiguration.id = :serviceConfigurationId AND templateVersionHealthStatus.status = 'UNHEALTHY'",
  )
  fun updateStatusToHealthyWhereUnhealthy(
    @Param("serviceConfigurationId") serviceConfigurationId: UUID,
    @Param("currentTime") currentTime: Instant,
  ): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE TemplateVersionHealthStatus templateVersionHealthStatus " +
      "SET templateVersionHealthStatus.status = 'UNHEALTHY', templateVersionHealthStatus.lastModified = :currentTime " +
      "WHERE templateVersionHealthStatus.serviceConfiguration.id = :serviceConfigurationId AND templateVersionHealthStatus.status = 'HEALTHY'",
  )
  fun updateStatusToUnhealthyWhereHealthy(
    @Param("serviceConfigurationId") serviceConfigurationId: UUID,
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
