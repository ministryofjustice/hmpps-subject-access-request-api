package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequestAdminSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SubjectAccessRequestService

@RestController
@Transactional
@PreAuthorize("hasAnyRole('ROLE_SAR_ADMIN_ACCESS')")
@RequestMapping("/api/admin")
class AdminController(val subjectAccessRequestService: SubjectAccessRequestService) {
  @GetMapping("/subjectAccessRequests")
  @Operation(summary = "Get Subject Access Request Admin Summary.", description = "Return Subject Access Request admin specific details.")
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
  @Parameter(name = "completed", description = "Include Subject Access Requests that are completed. Defaults to true.", required = false, example = "false")
  @Parameter(name = "errored", description = "Include Subject Access Requests that are errored. Defaults to true.", required = false, example = "false")
  @Parameter(name = "overdue", description = "Include Subject Access Requests that are overdue. Defaults to true.", required = false, example = "false")
  @Parameter(name = "pending", description = "Include Subject Access Requests that are pending. Defaults to true.", required = false, example = "false")
  @Parameter(name = "search", description = "If provided, only results containing this string in the case reference number or subject ID will be returned.", required = false, example = "A1234AA")
  @Parameter(name = "pageNumber", description = "The number of the page requested.", required = false, example = "1")
  @Parameter(name = "pageSize", description = "The number of results that make up a single page.", required = false, example = "20")
  fun getSubjectAccessRequests(
    @RequestParam(required = false, name = "completed") completed: Boolean = true,
    @RequestParam(required = false, name = "errored") errored: Boolean = true,
    @RequestParam(required = false, name = "overdue") overdue: Boolean = true,
    @RequestParam(required = false, name = "pending") pending: Boolean = true,
    @RequestParam(required = false, name = "search") search: String = "",
    @RequestParam(required = false, name = "pageNumber") pageNumber: Int? = null,
    @RequestParam(required = false, name = "pageSize") pageSize: Int? = null,
  ): SubjectAccessRequestAdminSummary = subjectAccessRequestService.getSubjectAccessRequestAdminSummary(completed, errored, overdue, pending, search, pageNumber, pageSize)
}
