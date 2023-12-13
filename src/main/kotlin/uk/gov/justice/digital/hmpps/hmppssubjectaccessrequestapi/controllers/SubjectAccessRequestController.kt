package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val auditService: AuditService) {
  @PostMapping("createSubjectAccessReport")
  fun createSubjectAccessReportPost(authentication: Authentication): String {
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REPORT", "Create Subject Access Report")
    return "MockId"
  }
}
