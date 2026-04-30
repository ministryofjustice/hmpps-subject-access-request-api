package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity
@Table(name = "SUBJECT_ACCESS_REQUEST_ARCHIVE")
data class ArchivedSubjectAccessRequest(
  // SAR fields
  @Id
  val id: UUID,

  @Column(name = "sar_id", nullable = false)
  val sarId: UUID,

  @Enumerated(EnumType.STRING)
  @Column(name = "sar_status", nullable = false)
  val sarStatus: Status = Status.Pending,

  @Column(name = "sar_date_from", nullable = false)
  val sarDateFrom: LocalDate? = null,

  @Column(name = "sar_date_to", nullable = false)
  var sarDateTo: LocalDate? = null,

  @Column(name = "sar_case_reference_number", nullable = false)
  val sarCaseReferenceNumber: String = "",

  @Column(name = "sar_nomis_id", nullable = false)
  val sarNomisId: String? = null,

  @Column(name = "sar_ndelius_case_reference_id", nullable = false)
  val sarNdeliusCaseReferenceId: String? = null,

  @Column(name = "sar_requested_by", nullable = false)
  val sarRequestedBy: String = "",

  @Column(name = "sar_request_date_time", nullable = false)
  val sarRequestDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),

  @Column(name = "sar_claim_date_time", nullable = false)
  val sarClaimDateTime: LocalDateTime? = null,

  @Column(name = "sar_claim_attempts", nullable = false)
  val sarClaimAttempts: Int = 0,

  @Column(name = "sar_object_url", nullable = false)
  val sarObjectUrl: String? = null,

  @Column(name = "sar_last_downloaded", nullable = false)
  val sarLastDownloaded: LocalDateTime? = null,

  // Service config fields
  @Column(name = "service_id", nullable = false)
  val serviceId: UUID,

  @Column(name = "service_name", nullable = false)
  val serviceName: String,

  @Column(name = "service_label", nullable = false)
  val serviceLabel: String,

  @Column(name = "service_url", nullable = false)
  val serviceUrl: String,

  @Column(name = "service_enabled", nullable = false)
  val serviceEnabled: Boolean = false,

  @Column(name = "service_template_migrated", nullable = false)
  val serviceTemplateMigrated: Boolean = false,

  @Enumerated(EnumType.STRING)
  @Column(name = "service_category", nullable = false)
  val serviceCategory: ServiceCategory? = null,

  @Column(name = "service_suspended", nullable = false)
  val serviceSuspended: Boolean = false,

  @Column(name = "service_suspended_at", nullable = false)
  var serviceSuspendedAt: Instant? = null,

  // Request service detail
  @Enumerated(EnumType.STRING)
  @Column(name = "request_render_status", nullable = false)
  val requestRenderStatus: RenderStatus,

  @Column(name = "request_rendered_at")
  val requestRenderedAt: LocalDateTime? = null,

  // templateVersion
  @Column(name = "template_version_id", nullable = false)
  val templateVersionId: UUID? = null,

  @Column(name = "template_version_status", nullable = false)
  val templateVersionStatus: TemplateVersionStatus? = null,

  @Column(name = "template_version_version", nullable = false)
  val templateVersion: Int? = -1,

  @Column(name = "template_version_created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
  var templateVersionCreatedAt: LocalDateTime? = null,

  @Column(name = "template_version_published_at", nullable = true, columnDefinition = "TIMESTAMP(6)")
  var templateVersionPublishedAt: LocalDateTime? = null,

  @Column(name = "template_version_file_hash", nullable = false)
  var templateVersionFileHash: String? = null,
) {
  constructor(
    requestServiceDetail: RequestServiceDetail,
  ) : this(
    id = UUID.randomUUID(),

    // SAR fields
    sarId = requestServiceDetail.subjectAccessRequest.id,
    sarStatus = requestServiceDetail.subjectAccessRequest.status,
    sarDateFrom = requestServiceDetail.subjectAccessRequest.dateFrom,
    sarDateTo = requestServiceDetail.subjectAccessRequest.dateTo,
    sarCaseReferenceNumber = requestServiceDetail.subjectAccessRequest.sarCaseReferenceNumber,
    sarNomisId = requestServiceDetail.subjectAccessRequest.nomisId,
    sarNdeliusCaseReferenceId = requestServiceDetail.subjectAccessRequest.ndeliusCaseReferenceId,
    sarRequestedBy = requestServiceDetail.subjectAccessRequest.requestedBy,
    sarRequestDateTime = requestServiceDetail.subjectAccessRequest.requestDateTime,
    sarClaimDateTime = requestServiceDetail.subjectAccessRequest.claimDateTime,
    sarClaimAttempts = requestServiceDetail.subjectAccessRequest.claimAttempts,
    sarObjectUrl = requestServiceDetail.subjectAccessRequest.objectUrl,
    sarLastDownloaded = requestServiceDetail.subjectAccessRequest.lastDownloaded,

    // Service configuration fields
    serviceId = requestServiceDetail.serviceConfiguration.id,
    serviceName = requestServiceDetail.serviceConfiguration.serviceName,
    serviceLabel = requestServiceDetail.serviceConfiguration.label,
    serviceUrl = requestServiceDetail.serviceConfiguration.url,
    serviceEnabled = requestServiceDetail.serviceConfiguration.enabled,
    serviceTemplateMigrated = requestServiceDetail.serviceConfiguration.templateMigrated,
    serviceCategory = requestServiceDetail.serviceConfiguration.category,
    serviceSuspended = requestServiceDetail.serviceConfiguration.suspended,
    serviceSuspendedAt = requestServiceDetail.serviceConfiguration.suspendedAt,

    // Request service detail
    requestRenderStatus = requestServiceDetail.renderStatus,
    requestRenderedAt = requestServiceDetail.renderedAt,

    // templateVersion fields
    templateVersionId = requestServiceDetail.templateVersion?.id,
    templateVersionStatus = requestServiceDetail.templateVersion?.status,
    templateVersion = requestServiceDetail.templateVersion?.version,
    templateVersionCreatedAt = requestServiceDetail.templateVersion?.createdAt,
    templateVersionPublishedAt = requestServiceDetail.templateVersion?.publishedAt,
    templateVersionFileHash = requestServiceDetail.templateVersion?.fileHash,
  )
}
