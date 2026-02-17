package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import java.util.UUID

@Repository
interface ServiceConfigurationRepository : JpaRepository<ServiceConfiguration, UUID> {
  fun findByOrderByServiceNameAsc(): List<ServiceConfiguration>?

  fun findAllByEnabledAndTemplateMigrated(
    enabled: Boolean = true,
    templateMigrated: Boolean = true,
  ): List<ServiceConfiguration>?

  fun findByServiceName(serviceName: String): ServiceConfiguration?

  fun deleteByServiceName(serviceName: String)
}
