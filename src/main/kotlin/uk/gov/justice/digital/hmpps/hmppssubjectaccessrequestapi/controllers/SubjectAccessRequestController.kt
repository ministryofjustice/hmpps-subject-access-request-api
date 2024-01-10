package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val auditService: AuditService, @Autowired val repo: SubjectAccessRequestRepository) {
  @PostMapping("createSubjectAccessRequest")
  fun createSubjectAccessRequestPost(@RequestBody request: String, authentication: Authentication, requestTime: LocalDateTime?): ResponseEntity<String> {
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", "Create Subject Access Request Report")
    val json = JSONObject(request)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateFrom = json.get("dateFrom").toString()
    val dateFromFormatted = LocalDate.parse(dateFrom, formatter)
    val dateTo = json.get("dateTo").toString()
    val dateToFormatted = LocalDate.parse(dateTo, formatter)
    if (json.has("nomisId") && json.has("ndeluisCaseReferenceId")) {
      if (json.get("nomisId") != "" && json.get("ndeliusCaseReferenceId") != "") {
        return ResponseEntity(
          "Both nomisId and ndeliusCaseReferenceId are provided - exactly one is required",
          HttpStatus.BAD_REQUEST
        )
      } else if (json.get("nomisId") == "" && json.get("ndeliusCaseReferenceId") == "") {
        return ResponseEntity(
          "Neither nomisId nor ndeliusCaseReferenceId is provided - exactly one is required",
          HttpStatus.BAD_REQUEST
        )
      }
    } else if (!json.has("nomisId") && !json.has("ndeluisCaseReferenceId")) {
      return ResponseEntity(
        "Neither nomisId nor ndeliusCaseReferenceId is provided - exactly one is required",
        HttpStatus.BAD_REQUEST
      )
    }


    repo.save(
      SubjectAccessRequest(
        id = null,
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = json.get("sarCaseReferenceNumber").toString(),
        services = json.get("services").toString(),
        nomisId = json.get("nomisId").toString(),
        ndeliusCaseReferenceId = json.get("ndeliusCaseReferenceId").toString(),
        requestedBy = authentication.name,
        requestDateTime = requestTime?: LocalDateTime.now(),
      ),
    )
    return ResponseEntity("", HttpStatus.OK); // Maybe want to return Report ID?
  }
}
