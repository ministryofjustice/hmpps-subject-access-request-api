package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository

@Component
class ServiceConfigurationService(private val serviceConfigurationRepository: ServiceConfigurationRepository) {
  fun getServiceConfigurationSanitised(): List<ServiceConfiguration>? =
    serviceConfigurationRepository.findByOrderByOrderAsc()
}
