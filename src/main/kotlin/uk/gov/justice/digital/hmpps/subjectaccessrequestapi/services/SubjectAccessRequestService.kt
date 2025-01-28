package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.ObjectUtils.isNotEmpty
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.DuplicateRequestResponseEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.CreateSubjectAccessRequestException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestApiException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.OverdueSubjectAccessRequests
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ReportsOverdueSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Service
class SubjectAccessRequestService(
  val documentStorageClient: DocumentStorageClient,
  val subjectAccessRequestRepository: SubjectAccessRequestRepository,
  val alertsConfiguration: AlertsConfiguration,
  private val telemetryClient: TelemetryClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun createSubjectAccessRequest(
    request: CreateSubjectAccessRequestEntity,
    requestedBy: String,
    requestTime: LocalDateTime?,
    id: UUID? = null,
  ): String {
    if (request.nomisId == null && request.ndeliusId == null) {
      throw CreateSubjectAccessRequestException(
        message = "Neither nomisId or nDeliusId provided - exactly one is required",
        status = HttpStatus.BAD_REQUEST,
        subjectAccessRequestId = id.toString(),
      )
    }

    if (isNotEmpty(request.nomisId?.trim()) && isNotEmpty(request.ndeliusId?.trim())) {
      throw CreateSubjectAccessRequestException(
        message = "Both nomisId and nDeliusId are provided - exactly one is required",
        status = HttpStatus.BAD_REQUEST,
        subjectAccessRequestId = id.toString(),
      )
    }

    val subjectAccessRequest = SubjectAccessRequest(
      id = id ?: UUID.randomUUID(),
      status = Status.Pending,
      dateFrom = request.dateFrom,
      dateTo = request.dateTo ?: LocalDate.now(),
      sarCaseReferenceNumber = request.sarCaseReferenceNumber!!,
      services = request.services!!,
      nomisId = request.nomisId,
      ndeliusCaseReferenceId = request.ndeliusId,
      requestedBy = requestedBy,
      requestDateTime = requestTime ?: LocalDateTime.now(),
    )

    subjectAccessRequestRepository.save(subjectAccessRequest)
    return subjectAccessRequest.id.toString()
  }

  fun getSubjectAccessRequests(
    unclaimedOnly: Boolean,
    search: String,
    pageNumber: Int? = null,
    pageSize: Int? = null,
  ): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      return subjectAccessRequestRepository.findUnclaimed(claimDateTime = LocalDateTime.now().minusMinutes(30))
    }

    var pagination = Pageable.unpaged(Sort.by("RequestDateTime").descending())
    if (pageNumber != null && pageSize != null) {
      pagination = PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending())
    }

    return subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
      caseReferenceSearch = search,
      nomisSearch = search,
      ndeliusSearch = search,
      pagination = pagination,
    ).content
  }

  fun claimSubjectAccessRequest(id: UUID) = subjectAccessRequestRepository.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
    id,
    LocalDateTime.now().minusMinutes(30),
    LocalDateTime.now(),
  )

  @Transactional
  fun completeSubjectAccessRequest(id: UUID): Int {
    return subjectAccessRequestRepository.findById(id).takeIf { it.isPresent }?.let { optional ->
      val subjectAccessRequest = optional.get()

      when (subjectAccessRequest.status) {
        Status.Pending -> {
          log.info("updating subject access request $id to status '${Status.Completed}'")
          return subjectAccessRequestRepository.updateStatus(id, Status.Completed)
        }

        Status.Completed -> {
          telemetryClient.trackApiEvent("DuplicateCompleteRequest", subjectAccessRequest.id.toString())
          return 0
        }

        Status.Errored -> {
          deleteFromDocumentStoreIfExists(subjectAccessRequest)
          throw SubjectAccessRequestApiException(
            message = "complete request unsuccessful existing status is '${Status.Errored}'",
            status = HttpStatus.BAD_REQUEST,
            subjectAccessRequestId = id.toString(),
          )
        }
      }
    } ?: throw SubjectAccessRequestApiException(
      message = "complete subject access request unsuccessful request ID not found",
      status = HttpStatus.NOT_FOUND,
      subjectAccessRequestId = id.toString(),
    )
  }

  /**
   * Attempt to delete the document from the document store intentionally swallows any non auth exceptions.
   */
  private fun deleteFromDocumentStoreIfExists(subjectAccessRequest: SubjectAccessRequest) {
    try {
      documentStorageClient.deleteDocument(subjectAccessRequest.id)
    } catch (ex: ClientAuthorizationException) {
      log.error("failed to obtain auth token for document API", ex)
      throw ex
    } catch (ex: Exception) {
      log.error("error while attempt to delete document from document store", ex)
    }
  }

  fun retrieveSubjectAccessRequestDocument(
    sarId: UUID,
    downloadDateTime: LocalDateTime? = LocalDateTime.now(),
  ): ResponseEntity<Flux<InputStreamResource>>? {
    log.info("Retrieving document in service")
    val document = documentStorageClient.retrieveDocument(sarId)
    log.info("Retrieved document")
    subjectAccessRequestRepository.updateLastDownloaded(sarId, downloadDateTime!!)
    telemetryClient.trackApiEvent(
      "ReportDocumentDownloadTimeUpdated",
      sarId.toString(),
      "downloadDateTime" to downloadDateTime.toString(),
    )
    log.info("Updated download time")
    return document
  }

  @Transactional
  fun getOverdueSubjectAccessRequestsSummary(): ReportsOverdueSummary {
    val threshold = alertsConfiguration.overdueAlertConfig.calculateOverdueThreshold()
    val overdue = subjectAccessRequestRepository.findAllPendingSubjectAccessRequestsSubmittedBefore(threshold)

    val overdueRequests = overdue.map {
      it?.let {
        OverdueSubjectAccessRequests(
          it.id,
          it.sarCaseReferenceNumber,
          it.requestDateTime,
          it.claimDateTime,
          it.claimAttempts,
          Duration.between(it.requestDateTime, LocalDateTime.now()),
        )
      }
    }
    return ReportsOverdueSummary(
      alertsConfiguration.overdueAlertConfig.thresholdAsString(),
      overdueRequests,
    )
  }

  @Transactional
  fun countPendingSubjectAccessRequests(): Int = subjectAccessRequestRepository.countSubjectAccessRequestsByStatus(Status.Pending)

  @Transactional()
  fun expirePendingRequestsSubmittedBeforeThreshold(): List<SubjectAccessRequest> {
    val threshold = alertsConfiguration.requestTimeoutAlertConfig.calculateTimeoutThreshold()
    val expiredRequests = mutableListOf<SubjectAccessRequest>()

    subjectAccessRequestRepository.findAllPendingSubjectAccessRequestsSubmittedBefore(threshold).forEach {
      it?.let { subjectAccessRequest ->
        subjectAccessRequestRepository.updateStatusToErrorSubmittedBefore(subjectAccessRequest.id, threshold)
          .takeIf { updated -> updated == 1 }?.let {
            expiredRequests.add(subjectAccessRequest)
          }
      }
    }
    return expiredRequests
  }

  /**
   * Create a new subject access request in state pending with details copied from original request. New request will
   * have zero claims attempts/null claim date and will be requestedBy the auth principal.
   */
  @Transactional
  fun duplicateSubjectAccessRequest(id: UUID): DuplicateRequestResponseEntity = findSubjectAccessRequest(id).takeIf { it.isPresent }?.let {
    val originalRequest = it.get()

    val resubmittedRequestId = copyAndSaveSubjectAccessRequest(originalRequest, getAuthenticationPrincipalName())

    telemetryClient.trackEvent(
      "subjectAccessRequestDuplicated",
      mapOf(
        "originalId" to originalRequest.id.toString(),
        "duplicatedRequestId" to resubmittedRequestId.toString(),
        "sarCaseReferenceNumber" to originalRequest.sarCaseReferenceNumber,
      ),
      null,
    )

    DuplicateRequestResponseEntity(
      id = resubmittedRequestId.toString(),
      originalId = originalRequest.id.toString(),
      sarCaseReferenceNumber = originalRequest.sarCaseReferenceNumber,
    )
  } ?: throw SubjectAccessRequestApiException(
    message = "duplicate subject access request unsuccessful: request ID not found",
    status = HttpStatus.NOT_FOUND,
    subjectAccessRequestId = id.toString(),
  )

  private fun findSubjectAccessRequest(id: UUID): Optional<SubjectAccessRequest> {
    try {
      return subjectAccessRequestRepository.findById(id)
    } catch (ex: Exception) {
      throw SubjectAccessRequestApiException(
        message = "unexpected error occurred while attempting to find request by id",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        subjectAccessRequestId = id.toString(),
      )
    }
  }

  private fun copyAndSaveSubjectAccessRequest(source: SubjectAccessRequest, requestedBy: String): UUID {
    try {
      return UUID.fromString(
        createSubjectAccessRequest(
          request = CreateSubjectAccessRequestEntity.from(source),
          requestedBy = requestedBy,
          requestTime = LocalDateTime.now(),
        ),
      )
    } catch (ex: Exception) {
      throw SubjectAccessRequestApiException(
        message = "unexpected error occurred while attempting to save subject access request",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
      )
    }
  }

  internal fun getAuthenticationPrincipalName() = SecurityContextHolder.getContext().authentication.name
}
