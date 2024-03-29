package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubjectAccessRequestService(
  @Autowired val sarDbGateway: SubjectAccessRequestGateway,
  @Autowired val documentStorageGateway: DocumentStorageGateway,
) {

  fun createSubjectAccessRequest(
    request: String,
    authentication: Authentication,
    requestTime: LocalDateTime?,
    id: UUID? = null,
  ): String {
    val json = JSONObject(request)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateFrom = json.get("dateFrom").toString()
    val dateFromFormatted = if (dateFrom != "") LocalDate.parse(dateFrom, formatter) else null
    val dateTo = json.get("dateTo").toString()
    val dateToFormatted = LocalDate.parse(dateTo, formatter)

    if (json.get("nomisId") != "" && json.get("ndeliusId") != "") {
      return "Both nomisId and ndeliusId are provided - exactly one is required"
    } else if (json.get("nomisId") == "" && json.get("ndeliusId") == "") {
      return "Neither nomisId nor ndeliusId is provided - exactly one is required"
    }
    sarDbGateway.saveSubjectAccessRequest(
      SubjectAccessRequest(
        id = id ?: UUID.randomUUID(),
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = json.get("sarCaseReferenceNumber").toString(),
        services = json.get("services").toString(),
        nomisId = json.get("nomisId").toString(),
        ndeliusCaseReferenceId = json.get("ndeliusId").toString(),
        requestedBy = authentication.name,
        requestDateTime = requestTime ?: LocalDateTime.now(),
      ),
    )
    return "" // Maybe want to return Report ID?
  }

  fun getSubjectAccessRequests(unclaimedOnly: Boolean): List<SubjectAccessRequest?> {
    val subjectAccessRequests = sarDbGateway.getSubjectAccessRequests(unclaimedOnly)
    return subjectAccessRequests
  }

  fun claimSubjectAccessRequest(id: UUID, time: LocalDateTime? = LocalDateTime.now()): Int {
    val thresholdTime = time!!.minusMinutes(5)
    return sarDbGateway.updateSubjectAccessRequestClaim(id, thresholdTime, time)
  }

  fun completeSubjectAccessRequest(id: UUID): Int {
    return sarDbGateway.updateSubjectAccessRequestStatusCompleted(id)
  }

  fun retrieveSubjectAccessRequestDocument(sarId: UUID): JSONObject? {
    val document = documentStorageGateway.retrieveDocument(sarId)
    return document
  }
}
