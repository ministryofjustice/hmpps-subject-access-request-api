package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubjectAccessRequestService(
  @Autowired val sarDbGateway: SubjectAccessRequestGateway,
  @Autowired val documentStorageGateway: DocumentStorageGateway,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun createSubjectAccessRequest(
    request: String,
    authentication: Authentication,
    requestTime: LocalDateTime?,
    id: UUID? = null,
  ): String {
    val json = JSONObject(request)
    val dateFrom = formatDate(json.get("dateFrom").toString())
    val dateTo = formatDate(json.get("dateTo").toString())

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
      return "Both nomisId and ndeliusId are provided - exactly one is required"
    }
    if (nomisId == null && ndeliusId == null) {
      return "Neither nomisId nor ndeliusId is provided - exactly one is required"
    }
    sarDbGateway.saveSubjectAccessRequest(
      SubjectAccessRequest(
        id = id ?: UUID.randomUUID(),
        status = Status.Pending,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = json.get("sarCaseReferenceNumber").toString(),
        services = json.get("services").toString(),
        nomisId = nomisId?.toString(),
        ndeliusCaseReferenceId = ndeliusId?.toString(),
        requestedBy = authentication.name,
        requestDateTime = requestTime ?: LocalDateTime.now(),
      ),
    )
    return "" // Maybe want to return Report ID?
  }

  fun getSubjectAccessRequests(unclaimedOnly: Boolean, search: String, pageNumber: Int? = null, pageSize: Int? = null): List<SubjectAccessRequest?> {
    val subjectAccessRequests = sarDbGateway.getSubjectAccessRequests(unclaimedOnly, search, pageNumber, pageSize)
    return subjectAccessRequests
  }

  fun claimSubjectAccessRequest(id: UUID, time: LocalDateTime? = LocalDateTime.now()): Int {
    val thresholdTime = time!!.minusMinutes(30)
    return sarDbGateway.updateSubjectAccessRequestClaim(id, thresholdTime, time)
  }

  fun completeSubjectAccessRequest(id: UUID): Int {
    return sarDbGateway.updateSubjectAccessRequestStatusCompleted(id)
  }

  fun retrieveSubjectAccessRequestDocument(sarId: UUID, downloadDateTime: LocalDateTime? = LocalDateTime.now()): ResponseEntity<Flux<InputStreamResource>>? {
    log.info("Retrieving document in service")
    val document = documentStorageGateway.retrieveDocument(sarId)
    log.info("Retrieved document")
    sarDbGateway.updateLastDownloadedDateTime(sarId, downloadDateTime!!)
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
