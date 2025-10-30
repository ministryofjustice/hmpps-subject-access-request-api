package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.TemplateVersionService
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/templates")
@PreAuthorize("hasAnyRole('ROLE_SAR_DATA_ACCESS', 'ROLE_SAR_SUPPORT')")
class TemplateVersionController(
  private val templateVersionService: TemplateVersionService,
  private val serviceConfigurationService: ServiceConfigurationService,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(TemplateVersionController::class.java)
  }

  @GetMapping("/service/{id}")
  @Operation(
    summary = "Get service template versions",
    description = "Get service template versions",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successful",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(arraySchema = Schema(implementation = TemplateVersionEntity::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised - user not authorised to get service template versions.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to get service template versions.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Service configuration ID not found",
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
  fun getTemplatesForService(
    @PathVariable("id") serviceId: UUID,
  ): ResponseEntity<List<TemplateVersionEntity>> = serviceConfigurationService.getById(serviceId)
    ?.let { serviceConfig ->
      val versions = templateVersionService.getVersions(serviceConfig.id)?.map { it.toEntity() } ?: emptyList()
      ResponseEntity.ok(versions)
    } ?: ResponseEntity.notFound().build()


  @PostMapping("/service/{id}")
  @Operation(
    summary = "Create new service template version",
    description = "Create new service template version",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Template version created successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = TemplateVersionEntity::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised - user not authorised to get service template versions.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to get service template versions.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Service configuration ID not found",
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
  fun newServiceTemplateVersion(
    @PathVariable("id") serviceId: UUID,
    @RequestParam("file") file: MultipartFile,
  ): ResponseEntity<TemplateVersionEntity> = serviceConfigurationService.getById(serviceId)
    ?.let { service ->
      file.bytes.takeIf { it.isNotEmpty() }
        ?.let { bytes ->
          val templateVersion = templateVersionService.newTemplateVersion(service.id, String(bytes))
          log.info(
            "created template version: {}, status: {}, service: {}",
            service.serviceName,
            templateVersion.status,
            templateVersion.version,
          )
          ResponseEntity.status(HttpStatus.CREATED).body(templateVersion.toEntity())
        } ?: run {
        log.error("create template version for service: {} unsuccessful: template body was empty", service.serviceName)
        ResponseEntity.badRequest().build()
      }
    } ?: run {
    log.error("create template version unsuccessful: service configuration not found for ID: {}", serviceId)
    ResponseEntity.notFound().build()
  }

  protected fun TemplateVersion.toEntity(): TemplateVersionEntity = TemplateVersionEntity(
    id = this.id,
    serviceName = this.serviceConfiguration?.serviceName,
    version = this.version,
    createdDate = this.createdAt,
    fileHash = this.fileHash,
    status = this.status,
  )
}

data class TemplateVersionEntity(
  val id: UUID? = null,
  val serviceName: String? = null,
  val version: Int? = null,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  val createdDate: LocalDateTime? = null,
  val fileHash: String? = null,
  val status: TemplateStatus? = null,
) {
  constructor() : this(null, null, null, null, null, TemplateStatus.PENDING)
}
