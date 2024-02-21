package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime

@RestController
@Transactional
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val subjectAccessRequestService: SubjectAccessRequestService, @Autowired val auditService: AuditService) {
  @PostMapping("createSubjectAccessRequest")
  fun createSubjectAccessRequest(@RequestBody request: String, authentication: Authentication, requestTime: LocalDateTime?): ResponseEntity<String> {
    val json = JSONObject(request)
    val nomisId = json.get("nomisId").toString()
    val ndeliusId = json.get("ndeliusId").toString()
    val auditDetails = "{\\\"nomisId\\\": $nomisId\\\"ndeliusId\\\"\": $ndeliusId}"
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", auditDetails)
    val response = subjectAccessRequestService.createSubjectAccessRequest(request, authentication, requestTime)
    return if (response == "") {
      ResponseEntity(response, HttpStatus.OK)
    } else {
      ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
  }

  @GetMapping("subjectAccessRequests")
  fun getSubjectAccessRequests(@RequestParam(required = false, name = "unclaimed") unclaimed: Boolean = false): List<SubjectAccessRequest?> {
    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed)
    // auditService.createEvent(SAR DEETS)
    return response
  }

  @PatchMapping("subjectAccessRequests/{id}/claim")
  fun claimSubjectAccessRequest(@PathVariable("id") id: Int): Int {
    val response = subjectAccessRequestService.claimSubjectAccessRequest(id)
    // auditService.createEvent(SAR DEETS)
    return if (response == 0) {
      400
    } else {
      200
    }
  }

  @PatchMapping("subjectAccessRequests/{id}/complete")
  fun completeSubjectAccessRequest(@PathVariable("id") id: Int): Int {
    val response = subjectAccessRequestService.completeSubjectAccessRequest(id)
    // auditService.createEvent(SAR DEETS)
    return if (response == 0) {
      400
    } else {
      200
    }
  }
}
