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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateServiceConfigurationEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService

@RestController
@RequestMapping("/api/services")
@PreAuthorize("hasAnyRole('ROLE_SAR_USER_ACCESS', 'ROLE_SAR_DATA_ACCESS', 'ROLE_SAR_SUPPORT', 'ROLE_SAR_REGISTER_TEMPLATE')")
class ServicesController(
  private val serviceConfigurationService: ServiceConfigurationService,
) {

  @PostMapping
  fun createServiceConfiguration(
    @RequestBody body: CreateServiceConfigurationEntity,
  ): ResponseEntity<ServiceInfo> {
    validateCreateServiceConfiguration(body)
    val result = serviceConfigurationService.createServiceConfiguration(body.toServiceConfiguration())
    return ResponseEntity<String>.status(HttpStatus.CREATED).body(ServiceInfo(serviceConfiguration = result))
  }

  private fun validateCreateServiceConfiguration(body: CreateServiceConfigurationEntity) {
    body.takeIf { it.name.isNullOrBlank() }?.let {
      throw ValidationException("create service configuration requires non null non empty Name value")
    }
    body.takeIf { it.label.isNullOrBlank() }?.let {
      throw ValidationException("create service configuration requires non null non empty Label value")
    }
    body.takeIf { it.url.isNullOrBlank() }?.let {
      throw ValidationException("create service configuration requires non null non empty URL value")
    }
    body.takeIf { it.category.isNullOrBlank() }?.let {
      throw ValidationException("create service configuration requires non null non empty Category value")
    }

    body.category?.let { ServiceCategory.valueOfOrNull(it) }
      ?: throw ValidationException("create service configuration invalid Category value")

    body.takeIf { it.enabled == null }?.let {
      throw ValidationException("create service configuration requires non null Enabled value")
    }

    body.takeIf { it.templateMigrated == null }?.let {
      throw ValidationException("create service configuration requires non null Template Migrated value")
    }
  }

  private fun CreateServiceConfigurationEntity.toServiceConfiguration() = ServiceConfiguration(
    serviceName = this.name!!,
    label = this.label!!,
    url = this.url!!,
    enabled = this.enabled!!,
    templateMigrated = this.templateMigrated!!,
    category = ServiceCategory.valueOf(this.category!!),
  )

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
}
