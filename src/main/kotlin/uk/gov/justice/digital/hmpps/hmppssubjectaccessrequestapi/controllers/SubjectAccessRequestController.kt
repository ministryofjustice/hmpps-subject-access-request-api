package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.HmppsSubjectAccessRequestApi
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.service.IReportService


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController(private val reportService: IReportService) {

  @PostMapping("createSubjectAccessReport")
  fun createSubjectAccessReportPost(report: Report): String {
    reportService.save(report)
    val result = deviceService.getDevicesByDeviceWearerId(deviceWearerId)
    return "MockId"
  }
}