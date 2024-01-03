package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import java.time.LocalDateTime
import java.time.Month

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val auditService: AuditService) {
  @Autowired
  lateinit var repo: SubjectAccessRequestRepository

  @PostMapping("createSubjectAccessRequest")
  fun createSubjectAccessRequestPost(authentication: Authentication): String {
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", "Create Subject Access Request Report")
    val dateFrom =
      LocalDateTime.of(2019, Month.MARCH, 28, 14, 33, 48)
    val dateTo =
      LocalDateTime.of(2020, Month.MARCH, 28, 14, 33, 48)
    val requestedDateTime =
      LocalDateTime.now()
    val claimDateTime =
      LocalDateTime.now()

    repo.save(
      SubjectAccessRequest(
        "14",
        "1",
        dateFrom,
        dateTo,
        "1",
        listOf("1", "2", "4"),
        "1",
        "1",
        "1",
        "1",
        "1",
        requestedDateTime,
        claimDateTime,
        "1",
        "1",
        1,
      ),
    )
    return "MockId" // Maybe want to return Report ID?
  }
}
