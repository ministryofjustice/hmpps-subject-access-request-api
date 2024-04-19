package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestReport
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.*

@RestController
@Transactional
@PreAuthorize("hasRole('ROLE_SAR_USER_ACCESS')")
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val subjectAccessRequestService: SubjectAccessRequestService, @Autowired val auditService: AuditService, val telemetryClient: TelemetryClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("subjectAccessRequest")
  @Operation(summary = "Create a Subject Access Request.", description = "Create a request for a Subject Access Request report.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created a Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to create a Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Failed to create a Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
    ],
  )
  @Parameter(name = "nomisId", description = "Subject's NOMIS prisoner number. Either nomisId OR ndeliusId is required.", required = false, example = "A1234BC")
  @Parameter(name = "ndeliusId", description = "Subject's nDelius case reference number. Either nomisId OR ndeliusId is required.", required = false, example = "A123456")
  @Parameter(name = "dateFrom", description = "Start date of the period of time the requested SAR report must cover.", required = false, example = "31/12/1999")
  @Parameter(name = "dateTo", description = "End date of the period of time the requested SAR report must cover.", required = false, example = "31/12/2000")
  @Parameter(name = "sarCaseReferenceNumber", description = "Case reference number of the Subject Access Request.", required = true, example = "exampleCaseReferenceNumber")
  @Parameter(name = "services", description = "List of services from which subject data must be retrieved.", required = true, example = "[\"service1, service1.prison.service.justice.gov.uk\"]")
  @Parameter(name = "skipAudit", description = "Skip audit - for testing only.", required = false, example = "true")
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

  @GetMapping("subjectAccessRequests")
  @Operation(summary = "Get Subject Access Requests.", description = "Return a list of Subject Access Requests.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to retrieve Subject Access Requests.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Failed to retrieve Subject Access Requests.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
    ],
  )
  @Parameter(name = "unclaimed", description = "Return only Subject Access Requests that are unclaimed by a worker for report generation. Defaults to false.", required = false, example = "false")
  fun getSubjectAccessRequests(@RequestParam(required = false, name = "unclaimed") unclaimed: Boolean = false): List<SubjectAccessRequest?> {
    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed)
    return response
  }

  @GetMapping("report")
  @Operation(summary = "Get Subject Access Request Report.", description = "Return a completed Subject Access Request Report.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to retrieve Subject Access Request Reports.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Failed to retrieve Subject Access Request Report.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
    ],
  )
  @Parameter(name = "id", description = "ID for the Subject Access Request Report to download.", required = true, example = "11111111-2222-3333-4444-555555555555")
  fun getReport(@RequestParam(required = true, name = "id") id: UUID): ResponseEntity<Any> {
    try {
      val response = subjectAccessRequestService.retrieveSubjectAccessRequestDocument(id)
      if (response === null) {
        return ResponseEntity("Report Not Found", HttpStatus.NOT_FOUND)
      }
      return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(MediaType.APPLICATION_PDF_VALUE))
        .body(InputStreamResource(response))
    } catch (exception: Exception) {
      return ResponseEntity(exception.message, HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @PatchMapping("subjectAccessRequests/{id}/claim")
  @Operation(summary = "Claim Subject Access Request.", description = "Claim a Subject Access Request for a limited time to generate the requested report.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully claimed Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Int::class),
            examples = [ExampleObject(1.toString())],
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Failed to claim Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
    ],
  )
  fun claimSubjectAccessRequest(@PathVariable("id") id: UUID): Int {
    telemetryClient.trackEvent(
      "claimSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.claimSubjectAccessRequest(id)
    return if (response == 0) {
      400
    } else {
      200
    }
  }

  @PatchMapping("subjectAccessRequests/{id}/complete")
  @Operation(summary = "Complete Subject Access Request.", description = "Mark a Subject Access Request as complete when the report has been successfully generated.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully completed Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Int::class),
            examples = [ExampleObject(1.toString())],
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Failed to complete Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unable to serve request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
    ],
  )
  fun completeSubjectAccessRequest(@PathVariable("id") id: UUID): Int {
    telemetryClient.trackEvent(
      "completeSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.completeSubjectAccessRequest(id)
    return if (response == 0) {
      400
    } else {
      200
    }
  }

  @GetMapping("reports")
  fun getSubjectAccessRequestReports(@RequestParam(required = true, name = "pageSize") pageSize: Int, @RequestParam(required = true, name = "pageNumber") pageNumber: Int): List<SubjectAccessRequestReport> {
    val response = subjectAccessRequestService.getAllReports(PageRequest.of(pageNumber, pageSize))
    return response
  }
}

@Serializable
data class AuditDetails(val nomisId: String, val ndeliusId: String)
