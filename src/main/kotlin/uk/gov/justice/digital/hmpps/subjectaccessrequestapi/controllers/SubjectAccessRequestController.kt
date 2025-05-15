package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackApiEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.DuplicateRequestResponseEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.BacklogSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.OverdueReportSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ReportsOverdueSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID

@RestController
@Transactional
@PreAuthorize("hasAnyRole('ROLE_SAR_USER_ACCESS', 'ROLE_SAR_DATA_ACCESS')")
@RequestMapping("/api")
class SubjectAccessRequestController(
  val subjectAccessRequestService: SubjectAccessRequestService,
  val telemetryClient: TelemetryClient,
  val alertsConfiguration: AlertsConfiguration,
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val overdueAlertConfig = alertsConfiguration.overdueAlertConfig
  private val backlogAlertConfig = alertsConfiguration.backlogAlertConfig

  @PostMapping("/subjectAccessRequest")
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
            schema = Schema(implementation = CreateSubjectAccessRequestEntity::class),
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
  fun createSubjectAccessRequest(
    @RequestBody request: CreateSubjectAccessRequestEntity,
    authentication: Authentication,
    requestTime: LocalDateTime?,
  ): ResponseEntity<String> {
    log.info("Creating SAR Request")

    telemetryClient.trackEvent(
      "createSubjectAccessRequest",
      mapOf(
        "nomisId" to (request.nomisId ?: ""),
        "ndeliusId" to (request.ndeliusId ?: ""),
        "requestTime" to requestTime.toString(),
      ),
    )
    val response = subjectAccessRequestService.createSubjectAccessRequest(
      request = request,
      requestedBy = authentication.name,
      requestTime = requestTime,
    )
    return ResponseEntity(response, HttpStatus.CREATED)
  }

  @Operation(
    summary = "Get a Subject Access Request.",
    description = "Get a Subject Access Request by ID",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully retrieve a Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SubjectAccessRequest::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to create a Subject Access Request.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateSubjectAccessRequestEntity::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Subject Access Request not found.",
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
  @GetMapping("/subjectAccessRequest/{id}")
  fun getSubjectAccessRequest(
    @PathVariable id: UUID,
  ): ResponseEntity<SubjectAccessRequest> = subjectAccessRequestService
    .findSubjectAccessRequest(id).takeIf { it.isPresent }
    ?.let { ResponseEntity(it.get(), HttpStatus.OK) }
    ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

  @GetMapping("/subjectAccessRequests")
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
  @Parameter(name = "search", description = "If provided, only results containing this string in the case reference number or subject ID will be returned.", required = false, example = "A1234AA")
  @Parameter(name = "pageNumber", description = "The number of the page requested.", required = false, example = "1")
  @Parameter(name = "pageSize", description = "The number of results that make up a single page.", required = false, example = "20")
  fun getSubjectAccessRequests(
    @RequestParam(required = false, name = "unclaimed") unclaimed: Boolean = false,
    @RequestParam(required = false, name = "search") search: String = "",
    @RequestParam(required = false, name = "pageNumber") pageNumber: Int? = null,
    @RequestParam(required = false, name = "pageSize") pageSize: Int? = null,
  ): List<SubjectAccessRequest?> {
    val response = subjectAccessRequestService.getSubjectAccessRequests(unclaimed, search, pageNumber, pageSize)
    return response
  }

  @GetMapping("/totalSubjectAccessRequests")
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
  @Parameter(name = "search", description = "If provided, only results containing this string in the case reference number or subject ID will be returned.", required = false, example = "A1234AA")
  fun getTotalSubjectAccessRequests(
    @RequestParam(required = false, name = "search") search: String = "",
  ): Int {
    val response = subjectAccessRequestService.getSubjectAccessRequests(false, search)
    return response.size
  }

  @GetMapping("/report")
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
  @Parameter(name = "id", description = "ID for the Subject Access Request Report to download.", required = true, example = "11111111-2222-3333-4444-555555555555")
  fun getReport(@RequestParam(required = true, name = "id") id: UUID): ResponseEntity<out Any?>? {
    log.info("Retrieving report for ID $id.")
    telemetryClient.trackApiEvent("ReportDownloadStarted", id.toString())
    val docResponse = subjectAccessRequestService.retrieveSubjectAccessRequestDocument(id)

    val docSize = docResponse?.headers?.contentLength ?: 0
    telemetryClient.trackApiEvent("ReportDocumentRetrieved", id.toString(), "docSize" to docSize.toString())
    log.info("Retrieved document")
    if (docResponse == null) {
      log.info("Null docResponse")
    }
    if (docResponse?.body == null) {
      log.info("Null docResponse.body")
    }
    log.info(docResponse.toString())
    log.info(docResponse?.body.toString())
    val fileStream = docResponse?.body?.blockFirst()
    log.info("Extracted file stream")
    if (docResponse === null) {
      log.info("No docResponse detected")
      telemetryClient.trackApiEvent("ReportDownloadFailed", id.toString())
      return ResponseEntity("Report Not Found", HttpStatus.NOT_FOUND)
    }
    log.info("Retrieval successful.")
    telemetryClient.trackApiEvent("ReportDownloadSuccessful", id.toString())
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(docResponse.headers.contentType?.toString() ?: ""))
      .body(fileStream)
  }

  @PatchMapping("/subjectAccessRequests/{id}/claim")
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

  @PatchMapping("/subjectAccessRequests/{id}/complete")
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

  @GetMapping("/subjectAccessRequests/overdue")
  @PreAuthorize("hasRole('ROLE_SAR_SUPPORT')")
  @Operation(
    summary = "(Dev Support) Get overdue Subject Access Requests.",
    description = "Returns a list of Subject Access Requests with status Pending that have exceeded the expected processing time threshold",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successful returns a list of overdue requests. Returns an empty list if no overdue request are identified",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ReportsOverdueSummary::class),
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
  fun listOverdueReports(): ReportsOverdueSummary = subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary()

  @GetMapping("/subjectAccessRequests/summary")
  @PreAuthorize("hasRole('ROLE_SAR_SUPPORT')")
  @Operation(
    summary = "(Dev Support) Get service status summary",
    description = "Returns service status summary to aid support and monitoring",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successful returns service summary",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ServiceSummary::class),
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
  fun getServiceSummary(): ServiceSummary = ServiceSummary(
    BacklogSummary(
      count = subjectAccessRequestService.countPendingSubjectAccessRequests(),
      alertThreshold = backlogAlertConfig.threshold,
      alertFrequency = backlogAlertConfig.thresholdAlertFrequency(),
    ),
    OverdueReportSummary(
      count = subjectAccessRequestService.getOverdueSubjectAccessRequestsSummary().total,
      alertThreshold = "status == pending && requestDateTime < (time.now - ${overdueAlertConfig.thresholdAsString()})",
      alertFrequency = overdueAlertConfig.thresholdAlertFrequency(),
    ),
  )

  @Operation(
    summary = "(Dev Support) duplicates a subject access request",
    description = "Create a new subject access request copying fields 'dateFrom', 'dateTo', 'sarCaseReferenceNumber', " +
      "'services', 'nomisId', 'ndeliusCaseReferenceId' from the original request. The new request will have status = " +
      "'Pending', claimAttempts = 0, claimDateTime = null and be requested by the principal name of the API caller.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successful duplicated the request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateRequestResponseEntity::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthenticated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unauthorized to execute request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Original request not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Problem attempting to duplicate the request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping("/subjectAccessRequests/{id}/duplicate")
  @PreAuthorize("hasRole('ROLE_SAR_SUPPORT')")
  fun resubmitRequest(@PathVariable("id") id: UUID): ResponseEntity<DuplicateRequestResponseEntity> = ResponseEntity(subjectAccessRequestService.duplicateSubjectAccessRequest(id), HttpStatus.CREATED)
}
