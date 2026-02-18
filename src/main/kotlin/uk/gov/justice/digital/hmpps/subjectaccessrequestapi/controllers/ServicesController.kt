package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceConfigurationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService
import java.util.UUID

@RestController
@RequestMapping("/api/services")
@PreAuthorize("hasAnyRole('ROLE_SAR_USER_ACCESS', 'ROLE_SAR_DATA_ACCESS', 'ROLE_SAR_SUPPORT', 'ROLE_SAR_REGISTER_TEMPLATE')")
class ServicesController(
  private val serviceConfigurationService: ServiceConfigurationService,
) {

  @PostMapping
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Create Service Configuration successful",
        content = [
          Content(
            mediaType = "application/json",
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request - invalid Service Configuration value",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to create service configuration",
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
  fun createServiceConfiguration(
    @RequestBody entity: ServiceConfigurationEntity,
  ): ResponseEntity<ServiceInfo> {
    validateServiceConfigurationEntity(entity)
    val result = serviceConfigurationService.createServiceConfiguration(entity.toServiceConfiguration())
    return ResponseEntity<String>.status(HttpStatus.CREATED).body(ServiceInfo(serviceConfiguration = result))
  }

  @PutMapping("/{id}")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Update Service Configuration successful",
        content = [
          Content(
            mediaType = "application/json",
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request - invalid Service Configuration value",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to create service configuration",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not Found - service configuration not found",
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
  fun updateServiceConfiguration(
    @PathVariable id: UUID,
    @RequestBody entity: ServiceConfigurationEntity,
  ): ResponseEntity<ServiceInfo> {
    validateServiceConfigurationEntity(entity)
    val updated = serviceConfigurationService.updateServiceConfiguration(
      createServiceConfigurationUpdate(
        id,
        entity,
      ),
    )
    return ResponseEntity.ok(ServiceInfo(updated))
  }

  @GetMapping
  @Operation(
    summary = "Get the Services list",
    description = "Get the Services list",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successful",
        content = [
          Content(
            mediaType = "application/json",
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - user not authorised to get services list",
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
  fun getServices(): ResponseEntity<List<ServiceInfo>> {
    val services: List<ServiceInfo>? = serviceConfigurationService.getServiceConfigurationSanitised()?.map {
      ServiceInfo(serviceConfiguration = it)
    }
    return ResponseEntity(services, HttpStatus.OK)
  }

  private fun validateServiceConfigurationEntity(entity: ServiceConfigurationEntity) {
    if (entity.name.isNullOrBlank()) {
      throw ValidationException("create service configuration requires non null non empty Name value")
    }
    if (entity.label.isNullOrBlank()) {
      throw ValidationException("create service configuration requires non null non empty Label value")
    }
    if (entity.url.isNullOrBlank()) {
      throw ValidationException("create service configuration requires non null non empty URL value")
    }
    if (entity.category.isNullOrBlank()) {
      throw ValidationException("create service configuration requires non null non empty Category value")
    }
    ServiceCategory.valueOfOrNull(entity.category)
      ?: throw ValidationException("create service configuration invalid Category value")

    if (entity.enabled == null) {
      throw ValidationException("create service configuration requires non null Enabled value")
    }
    if (entity.templateMigrated == null) {
      throw ValidationException("create service configuration requires non null Template Migrated value")
    }
  }

  private fun ServiceConfigurationEntity.toServiceConfiguration(
    id: UUID = UUID.randomUUID(),
  ) = ServiceConfiguration(
    id = id,
    serviceName = this.name!!,
    label = this.label!!,
    url = this.url!!,
    enabled = this.enabled!!,
    templateMigrated = this.templateMigrated!!,
    category = ServiceCategory.valueOf(this.category!!),
  )

  private fun createServiceConfigurationUpdate(
    id: UUID,
    entity: ServiceConfigurationEntity,
  ) = ServiceConfigurationService.ServiceConfigurationUpdate(
    id = id,
    serviceName = entity.name!!,
    label = entity.label!!,
    url = entity.url!!,
    enabled = entity.enabled!!,
    templateMigrated = entity.templateMigrated!!,
    category = ServiceCategory.valueOf(entity.category!!),
  )
}
