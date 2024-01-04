package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import com.nimbusds.jose.shaded.gson.JsonObject
//import net.minidev.json.JSONObject
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
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
  fun createSubjectAccessRequestPost(@RequestBody request: String, authentication: Authentication): String {
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", "Create Subject Access Request Report")
    var json = JSONObject(request)
    println(json.get("request"))
    val dateFrom =
      LocalDateTime.of(2019, Month.MARCH, 28, 14, 33, 48)
    val dateTo =
      LocalDateTime.of(2020, Month.MARCH, 28, 14, 33, 48)
    val requestedDateTime =
      LocalDateTime.now()

    repo.save(
      SubjectAccessRequest(
        id = null,
        status = Status.Pending,
        dateFrom = dateFrom,
        dateTo = dateTo,
        sarCaseReferenceNumber = "1",
        services = "1,2,4",
        nomisId = "1",
        ndeliusCaseReferenceId = "1",
        requestedBy = "1",
        requestDateTime = requestedDateTime,
        objectUrl = "1",
      ),
    )
    return "MockId" // Maybe want to return Report ID?
  }
}
