package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import java.util.UUID

@Repository
interface TemplateVersionRepository : JpaRepository<TemplateVersion, UUID> {
  fun deleteByServiceConfigurationIdAndStatus(id: UUID, status: TemplateVersionStatus)
  fun findByServiceConfigurationIdOrderByVersionDesc(id: UUID): List<TemplateVersion>

  @Query(
    "SELECT template FROM TemplateVersion template " +
      "WHERE template.serviceConfiguration.id = :id " +
      "ORDER BY template.createdAt DESC " +
      "LIMIT 1",
  )
  fun findLatestByServiceConfigurationId(@Param("id") id: UUID): TemplateVersion?
}
