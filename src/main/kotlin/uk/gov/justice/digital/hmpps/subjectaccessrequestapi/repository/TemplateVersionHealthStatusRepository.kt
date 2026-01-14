package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.util.UUID

@Repository
interface TemplateVersionHealthStatusRepository : JpaRepository<TemplateVersionHealthStatus, UUID>
