package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService

@RestController
@RequestMapping("/api/services")
@PreAuthorize("hasAnyRole('ROLE_SAR_USER_ACCESS', 'ROLE_SAR_DATA_ACCESS', 'ROLE_SAR_SUPPORT')")
class ServicesController(
  private val serviceConfigurationService: ServiceConfigurationService,
) {

  @GetMapping
  fun getServices(): ResponseEntity<List<ServiceInfo>> {
    val services: List<ServiceInfo>? = serviceConfigurationService.getServiceConfigurationSanitised()?.map {
      ServiceInfo(it.id, it.serviceName, it.label, it.url, it.order)
    }
    return ResponseEntity(services, HttpStatus.OK)
  }
}
