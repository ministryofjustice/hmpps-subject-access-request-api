package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.*

@RestController
@Transactional
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val subjectAccessRequestService: SubjectAccessRequestService, @Autowired val auditService: AuditService, val telemetryClient: TelemetryClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("createSubjectAccessRequest")
  @Operation(summary = "Create a Subject Access Request.", description = "Returns a person.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Successfully created a SAR report request with the information provided.",
        content = [
          Content(
            mediaType = "application/json"
          )
        ]
      ),
      ApiResponse(
        responseCode = "404", description = "Failed to create a SAR report request with the information provided.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "500", description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class)
          )
        ]
      ),
    ]
  )
  fun createSubjectAccessRequest(@RequestBody request: String, authentication: Authentication, requestTime: LocalDateTime?): ResponseEntity<String> {
    log.info("Creating SAR Request")
    val json = JSONObject(request)
    val nomisId = json.get("nomisId").toString()
    val ndeliusId = json.get("ndeliusId").toString()
    telemetryClient.trackEvent(
      "createSubjectAccessRequest",
      mapOf(
        "nomisId" to nomisId,
        "ndeliusId" to ndeliusId,
        "requestTime" to requestTime.toString(),
      ),
    )
    val auditDetails = Json.encodeToString(AuditDetails(nomisId, ndeliusId))
    auditService.createEvent(authentication.name, "CREATE_SUBJECT_ACCESS_REQUEST", auditDetails)
    val response = subjectAccessRequestService.createSubjectAccessRequest(
      request = request,
      authentication = authentication,
      requestTime = requestTime,
    )
    return if (response == "") {
      ResponseEntity(response, HttpStatus.OK)
    } else {
      ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
  }

//  @Operation(summary = "Get Subject Access Requests.", description = "Returns a list of Subject Access Requests.")
//  @ApiResponses(
//    value = [
//      ApiResponse(
//        responseCode = "200", description = "Successfully returned a list of Subject Access Requests.",
//        content = [
//          Content(
//            mediaType = "application/json"
//          )
//        ]
//      ),
//    ]
//  )
  @GetMapping("subjectAccessRequests")
  fun getSubjectAccessRequests(@RequestParam(required = false, name = "unclaimed") unclaimed: Boolean = false): List<SubjectAccessRequest?> {
    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed)
    // auditService.createEvent(SAR DEETS)
    return response
  }

  @Operation(summary = "Claim Subject Access Request.", description = "Update claim attempts and claim datetime of a Subject Access Request.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Successfully returned a list of Subject Access Requests.",
        content = [
          Content(
            mediaType = "application/json"
          )
        ]
      ),
    ]
  )
  @PatchMapping("subjectAccessRequests/{id}/claim")
  fun claimSubjectAccessRequest(@PathVariable("id") id: UUID): Int {
    telemetryClient.trackEvent(
      "claimSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.claimSubjectAccessRequest(id)
    // auditService.createEvent(SAR DEETS)
    return if (response == 0) {
      400
    } else {
      200
    }
  }

  @PatchMapping("subjectAccessRequests/{id}/complete")
  fun completeSubjectAccessRequest(@PathVariable("id") id: UUID): Int {
    telemetryClient.trackEvent(
      "completeSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.completeSubjectAccessRequest(id)
    // auditService.createEvent(SAR DEETS)
    return if (response == 0) {
      400
    } else {
      200
    }
  }
}

@Serializable
data class AuditDetails(val nomisId: String, val ndeliusId: String)
