package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController {

  @PostMapping("createSubjectAccessReport")
  fun createSubjectAccessReportPost(): String {
    return "MockId"
  }
}
