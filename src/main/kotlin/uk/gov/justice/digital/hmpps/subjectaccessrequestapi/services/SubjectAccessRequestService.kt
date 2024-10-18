package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubjectAccessRequestService(
  @Autowired val documentStorageGateway: DocumentStorageGateway,
  @Autowired val subjectAccessRequestRepository: SubjectAccessRequestRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun createSubjectAccessRequest(
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

  fun getSubjectAccessRequests(unclaimedOnly: Boolean, search: String, pageNumber: Int? = null, pageSize: Int? = null): List<SubjectAccessRequest?> {
    if (unclaimedOnly) {
      return subjectAccessRequestRepository.findUnclaimed(claimDateTime = LocalDateTime.now().minusMinutes(30))
    }

    var pagination = Pageable.unpaged(Sort.by("RequestDateTime").descending())
    if (pageNumber != null && pageSize != null) {
      pagination = PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending())
    }

    return subjectAccessRequestRepository.findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(caseReferenceSearch = search, nomisSearch = search, ndeliusSearch = search, pagination = pagination).content
  }

  fun claimSubjectAccessRequest(id: UUID) = subjectAccessRequestRepository.updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(id, LocalDateTime.now().minusMinutes(30), LocalDateTime.now())

  fun completeSubjectAccessRequest(id: UUID) = subjectAccessRequestRepository.updateStatus(id, Status.Completed)

  fun retrieveSubjectAccessRequestDocument(sarId: UUID, downloadDateTime: LocalDateTime? = LocalDateTime.now()): ResponseEntity<Flux<InputStreamResource>>? {
    log.info("Retrieving document in service")
    val document = documentStorageGateway.retrieveDocument(sarId)
    log.info("Retrieved document")
    subjectAccessRequestRepository.updateLastDownloaded(sarId, downloadDateTime!!)
    log.info("Updated download time")
    return document
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
