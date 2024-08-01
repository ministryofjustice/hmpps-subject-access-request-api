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
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.*

@RestController
@Transactional
@PreAuthorize("hasAnyRole('ROLE_SAR_USER_ACCESS', 'ROLE_SAR_DATA_ACCESS')")
@RequestMapping("/api/")
class SubjectAccessRequestController(@Autowired val subjectAccessRequestService: SubjectAccessRequestService, @Autowired val auditService: AuditService, val telemetryClient: TelemetryClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("subjectAccessRequest")
  @Operation(
    summary = "Create a Subject Access Request.",
    description = "Create a request for a Subject Access Request report.",
  )
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
  @Parameter(
    name = "nomisId",
    description = "Subject's NOMIS prisoner number. Either nomisId OR ndeliusId is required.",
    required = false,
    example = "A1234BC",
  )
  @Parameter(
    name = "ndeliusId",
    description = "Subject's nDelius case reference number. Either nomisId OR ndeliusId is required.",
    required = false,
    example = "A123456",
  )
  @Parameter(
    name = "dateFrom",
    description = "Start date of the period of time the requested SAR report must cover.",
    required = false,
    example = "31/12/1999",
  )
  @Parameter(
    name = "dateTo",
    description = "End date of the period of time the requested SAR report must cover.",
    required = false,
    example = "31/12/2000",
  )
  @Parameter(
    name = "sarCaseReferenceNumber",
    description = "Case reference number of the Subject Access Request.",
    required = true,
    example = "exampleCaseReferenceNumber",
  )
  @Parameter(
    name = "services",
    description = "List of services from which subject data must be retrieved.",
    required = true,
    example = "[\"service1, service1.prison.service.justice.gov.uk\"]",
  )
  fun createSubjectAccessRequest(
    @RequestBody request: String,
    authentication: Authentication,
    requestTime: LocalDateTime?,
  ): ResponseEntity<String> {
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
  @Parameter(
    name = "unclaimed",
    description = "Return only Subject Access Requests that are unclaimed by a worker for report generation. Defaults to false.",
    required = false,
    example = "false",
  )
  @Parameter(
    name = "search",
    description = "If provided, only results containing this string in the case reference number or subject ID will be returned.",
    required = false,
    example = "A1234AA",
  )
  @Parameter(
    name = "pageNumber",
    description = "The number of the page requested.",
    required = false,
    example = "1",
  )
  @Parameter(
    name = "pageSize",
    description = "The number of results that make up a single page.",
    required = false,
    example = "20",
  )
  fun getSubjectAccessRequests(
    @RequestParam(
      required = false,
      name = "unclaimed",
    ) unclaimed: Boolean = false,
    @RequestParam(
      required = false,
      name = "search",
    ) search: String = "",
    @RequestParam(
      required = false,
      name = "pageNumber",
    ) pageNumber: Int? = null,
    @RequestParam(
      required = false,
      name = "pageSize",
    ) pageSize: Int? = null,
  ): List<SubjectAccessRequest?> {
    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed, search, pageNumber, pageSize)
    return response
  }

  @GetMapping("totalSubjectAccessRequests")
  @Operation(summary = "Get total number of Subject Access Requests.", description = "Return the number of Subject Access Requests.")
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
  @Parameter(
    name = "search",
    description = "If provided, only results containing this string in the case reference number or subject ID will be returned.",
    required = false,
    example = "A1234AA",
  )
  fun getTotalSubjectAccessRequests(
    @RequestParam(
      required = false,
      name = "search",
    ) search: String = "",
  ): Int {
    val response = subjectAccessRequestService.getSubjectAccessRequests(false, search)
    return response.size
  }

  @GetMapping("report")
  @Operation(
    summary = "Get Subject Access Request Report.",
    description = "Return a completed Subject Access Request Report.",
  )
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
  @Parameter(
    name = "id",
    description = "ID for the Subject Access Request Report to download.",
    required = true,
    example = "11111111-2222-3333-4444-555555555555",
  )
  fun getReport(@RequestParam(required = true, name = "id") id: UUID): ResponseEntity<out Any?>? {
    log.info("Retrieving report for ID $id.")
    val docResponse = subjectAccessRequestService.retrieveSubjectAccessRequestDocument(id)
    val fileStream = docResponse?.body?.blockFirst()
    if (docResponse === null) {
      return ResponseEntity("Report Not Found", HttpStatus.NOT_FOUND)
    }
    log.info("Retrieval successful.")
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(docResponse.headers.contentType?.toString() ?: ""))
      .body(fileStream)
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
  fun claimSubjectAccessRequest(@PathVariable("id") id: UUID): ResponseEntity<String> {
    telemetryClient.trackEvent(
      "claimSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.claimSubjectAccessRequest(id)
    return if (response == 0) {
      ResponseEntity(HttpStatus.BAD_REQUEST)
    } else {
      ResponseEntity(HttpStatus.OK)
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
  fun completeSubjectAccessRequest(@PathVariable("id") id: UUID): ResponseEntity<String> {
    telemetryClient.trackEvent(
      "completeSubjectAccessRequest",
      mapOf(
        "id" to id.toString(),
      ),
    )
    val response = subjectAccessRequestService.completeSubjectAccessRequest(id)
    return if (response == 0) {
      ResponseEntity(HttpStatus.BAD_REQUEST)
    } else {
      ResponseEntity(HttpStatus.OK)
    }
  }

  @PostMapping("deleteSubjectAccessRequests")
  @Operation(
    summary = "Delete old Subject Access Requests.",
    description = "Delete old requests for a Subject Access Request report.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully deleted Subject Access Requests.",
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
        description = "Failed to delete Subject Access Requests.",
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
  fun deleteOldSubjectAccessRequests(): ResponseEntity<String> {
    telemetryClient.trackEvent(
      "deleteOldSubjectAccessRequests",
    )
    val response = subjectAccessRequestService.deleteOldSubjectAccessRequests()
    return if (response == 0) {
      ResponseEntity(HttpStatus.BAD_REQUEST)
    } else {
      ResponseEntity(HttpStatus.OK)
    }
  }
}

@Serializable
data class AuditDetails(val nomisId: String, val ndeliusId: String)
