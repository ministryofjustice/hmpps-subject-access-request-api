package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val subjectAccessRequestService: SubjectAccessRequestService, @Autowired val auditService: AuditService) {
  @PostMapping("createSubjectAccessRequest")
  fun createSubjectAccessRequestPost(@RequestBody request: String, authentication: Authentication, requestTime: LocalDateTime?): ResponseEntity<String> {
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", "Create Subject Access Request Report")
    val response = subjectAccessRequestService.createSubjectAccessRequestPost(request, authentication, requestTime)
    return response

  }

  @GetMapping("subjectAccessRequest")
  fun getSubjectAccessRequests(@RequestParam(required = false, name = "unclaimed") unclaimed: Boolean = false): List<SubjectAccessRequest?> {

    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed)

    //auditService.createEvent(SAR DEETS)
    return response
  }
}
