package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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

  /**
   * Return the service configurations in the report presentation order:
   * - 'G' services sorted alphabetically,
   * - Prison services sorted alphabetically,
   * - Probation services sorted alphabetically.
   */
  @Query(
    value = """
      SELECT * FROM (
           SELECT a.*, 0 AS grp, a.label AS ord_name
           FROM service_configuration a
           WHERE a.service_name IN ('G1', 'G2', 'G3')
  
           UNION ALL
  
           SELECT b.*, 1 AS grp, b.label AS ord_name
           FROM service_configuration b
           WHERE b.service_name NOT IN ('G1', 'G2', 'G3') AND b.category = 'PRISON'
  
           UNION ALL
  
           SELECT c.*, 2 AS grp, c.label AS ord_name
           FROM service_configuration c
           WHERE c.service_name NOT IN ('G1', 'G2', 'G3') AND c.category = 'PROBATION'
     ) t ORDER BY grp ASC, ord_name ASC
    """,
    nativeQuery = true,
  )
  fun findAllReportOrdering(): List<ServiceConfiguration>
}
