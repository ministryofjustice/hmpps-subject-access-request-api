package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.ObjectUtils.isNotEmpty
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.CreateSubjectAccessRequestException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.OverdueSubjectAccessRequests
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ReportsOverdueSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubjectAccessRequestService(
  val documentStorageClient: DocumentStorageClient,
  val subjectAccessRequestRepository: SubjectAccessRequestRepository,
  val alertsConfiguration: AlertsConfiguration,
  private val telemetryClient: TelemetryClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun createSubjectAccessRequest2(
    request: String,
    requestedBy: String,
    requestTime: LocalDateTime?,
    id: UUID? = null,
  ): String {
    val json = JSONObject(request)
    val dateFrom = formatDate(json.get("dateFrom").toString())
    var dateTo = formatDate(json.get("dateTo").toString())

    var nomisId = json.get("nomisId")
    var ndeliusId = json.get("ndeliusId")

    val nullClassName = "org.json.JSONObject\$Null"
    if (nomisId.javaClass.name == nullClassName) {
      nomisId = null
    }
    if (ndeliusId.javaClass.name == nullClassName) {
      ndeliusId = null
    }

    if (nomisId != null && ndeliusId != null) {
      return "Both nomisId and nDeliusId are provided - exactly one is required"
    }
    if (nomisId == null && ndeliusId == null) {
      return "Neither nomisId nor nDeliusId is provided - exactly one is required"
    }
    if (dateTo == null) {
      dateTo = LocalDate.now()
    }
    subjectAccessRequestRepository.save(
      SubjectAccessRequest(
        id = id ?: UUID.randomUUID(),
        status = Status.Pending,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = json.get("sarCaseReferenceNumber").toString(),
        services = json.get("services").toString(),
        nomisId = nomisId?.toString(),
        ndeliusCaseReferenceId = ndeliusId?.toString(),
        requestedBy = requestedBy,
        requestDateTime = requestTime ?: LocalDateTime.now(),
      ),
    )
    return "" // Maybe want to return Report ID?
  }

  fun createSubjectAccessRequest(
    request: CreateSubjectAccessRequestEntity,
    requestedBy: String,
    requestTime: LocalDateTime?,
    id: UUID? = null,
  ): String {
    if (request.nomisId == null && request.ndeliusId == null) {
      throw CreateSubjectAccessRequestException(
        "Neither nomisId or nDeliusId provided - exactly one is required",
        HttpStatus.BAD_REQUEST,
      )
    }

    if (isNotEmpty(request.nomisId?.trim()) && isNotEmpty(request.ndeliusId?.trim())) {
      throw CreateSubjectAccessRequestException(
        "Both nomisId and nDeliusId are provided - exactly one is required",
        HttpStatus.BAD_REQUEST,
      )
    }

    if (request.dateTo == null) {
      request.dateTo = LocalDate.now()
    }

    val subjectAccessRequest = SubjectAccessRequest(
      id = id ?: UUID.randomUUID(),
      status = Status.Pending,
      dateFrom = request.dateFrom,
      dateTo = request.dateTo,
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

  fun claimSubjectAccessRequest(id: UUID) =
    subjectAccessRequestRepository.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
      id,
      LocalDateTime.now().minusMinutes(30),
      LocalDateTime.now(),
    )

  fun completeSubjectAccessRequest(id: UUID) = subjectAccessRequestRepository.updateStatus(id, Status.Completed)

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

  fun getOverdueSubjectAccessRequestsSummary(): ReportsOverdueSummary {
    val threshold = alertsConfiguration.calculateOverdueThreshold()
    val overdue = subjectAccessRequestRepository.findOverdueSubjectAccessRequests(threshold)

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
      alertsConfiguration.overdueThresholdAsString(),
      overdueRequests,
    )
  }

  fun countPendingSubjectAccessRequests(): Int {
    return subjectAccessRequestRepository.countSubjectAccessRequestsByStatus(Status.Pending)
  }
}

private fun formatDate(date: String): LocalDate? {
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  return if (date != "") {
    LocalDate.parse(date, formatter)
  } else {
    null
  }
}
