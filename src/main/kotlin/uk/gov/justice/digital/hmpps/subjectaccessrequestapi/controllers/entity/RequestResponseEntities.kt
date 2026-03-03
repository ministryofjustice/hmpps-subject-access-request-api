package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Duplicate subject access request API response entity
 */
data class DuplicateRequestResponseEntity(
  val id: String,
  val originalId: String,
  val sarCaseReferenceNumber: String,
)

/**
 * Create subject access request API response entity
 */
data class CreateSubjectAccessRequestEntity(
  val nomisId: String? = null,

  val ndeliusId: String? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  val dateFrom: LocalDate? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  var dateTo: LocalDate? = null,

  val sarCaseReferenceNumber: String? = null,

  val services: List<String> = emptyList(),
) {

  companion object {
    fun from(subjectAccessRequest: SubjectAccessRequest): CreateSubjectAccessRequestEntity = CreateSubjectAccessRequestEntity(
      nomisId = subjectAccessRequest.nomisId,
      ndeliusId = subjectAccessRequest.ndeliusCaseReferenceId,
      dateFrom = subjectAccessRequest.dateFrom,
      dateTo = subjectAccessRequest.dateTo,
      sarCaseReferenceNumber = subjectAccessRequest.sarCaseReferenceNumber,
      services = subjectAccessRequest.services.map { it.serviceConfiguration.serviceName },
    )
  }
}

data class ServiceInfo(
  val id: UUID,
  val name: String,
  val label: String,
  val url: String,
  val enabled: Boolean,
  val templateMigrated: Boolean,
  val category: ServiceCategory,
  val suspended: Boolean,
  val suspendedAt: Instant? = null,
) {

  constructor(serviceConfiguration: ServiceConfiguration) : this(
    id = serviceConfiguration.id,
    name = serviceConfiguration.serviceName,
    label = serviceConfiguration.label,
    url = serviceConfiguration.url,
    enabled = serviceConfiguration.enabled,
    templateMigrated = serviceConfiguration.templateMigrated,
    category = serviceConfiguration.category,
    suspended = serviceConfiguration.suspended,
    suspendedAt = serviceConfiguration.suspendedAt,
  )
}

data class ServiceConfigurationEntity(
  val name: String?,
  val label: String?,
  val url: String?,
  val category: String?,
  val enabled: Boolean?,
  val templateMigrated: Boolean?,
)

data class SubjectAccessRequestResponseEntity(
  val id: UUID,
  val status: Status,
  val dateFrom: LocalDate? = null,
  var dateTo: LocalDate? = null,
  val sarCaseReferenceNumber: String,
  val services: List<RequestServiceDetailResponseEntity>,
  val nomisId: String? = null,
  val ndeliusCaseReferenceId: String? = null,
  val requestedBy: String,
  val requestDateTime: LocalDateTime,
  val claimDateTime: LocalDateTime? = null,
  val claimAttempts: Int,
  val objectUrl: String? = null,
  val lastDownloaded: LocalDateTime? = null,
) {
  constructor(subjectAccessRequest: SubjectAccessRequest) : this(
    id = subjectAccessRequest.id,
    status = subjectAccessRequest.status,
    dateFrom = subjectAccessRequest.dateFrom,
    dateTo = subjectAccessRequest.dateTo,
    sarCaseReferenceNumber = subjectAccessRequest.sarCaseReferenceNumber,
    services = subjectAccessRequest.services.map { RequestServiceDetailResponseEntity(it) },
    nomisId = subjectAccessRequest.nomisId,
    ndeliusCaseReferenceId = subjectAccessRequest.ndeliusCaseReferenceId,
    requestedBy = subjectAccessRequest.requestedBy,
    requestDateTime = subjectAccessRequest.requestDateTime,
    claimDateTime = subjectAccessRequest.claimDateTime,
    claimAttempts = subjectAccessRequest.claimAttempts,
    objectUrl = subjectAccessRequest.objectUrl,
    lastDownloaded = subjectAccessRequest.lastDownloaded,
  )
}

data class RequestServiceDetailResponseEntity(
  val serviceName: String,
  val renderStatus: RenderStatus,
  val templateVersion: String? = null,
  val renderedAt: LocalDateTime? = null,
) {
  constructor(requestServiceDetail: RequestServiceDetail) : this(
    serviceName = requestServiceDetail.serviceConfiguration.serviceName,
    renderStatus = requestServiceDetail.renderStatus,
    templateVersion = requestServiceDetail.templateVersion,
    renderedAt = requestServiceDetail.renderedAt,
  )
}
