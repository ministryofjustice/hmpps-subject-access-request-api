package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.utils.ServiceConfigurationComparator
import java.util.UUID

@Service
class ServiceConfigurationService(private val serviceConfigurationRepository: ServiceConfigurationRepository) {

  fun getServiceConfigurationSanitised(): List<ServiceConfiguration>? = serviceConfigurationRepository
    .findAll()
    .sortedWith(ServiceConfigurationComparator())

  fun getById(id: UUID): ServiceConfiguration? = serviceConfigurationRepository.findByIdOrNull(id)

  fun getByServiceName(
    serviceName: String,
  ): ServiceConfiguration? = serviceConfigurationRepository.findByServiceName(serviceName)

  fun createServiceConfiguration(
    serviceConfiguration: ServiceConfiguration,
  ): ServiceConfiguration = serviceConfigurationRepository.findByServiceName(serviceConfiguration.serviceName)?.let {
    throw ValidationException("Service configuration with name ${serviceConfiguration.serviceName} already exists")
  } ?: run {
    serviceConfigurationRepository.saveAndFlush(serviceConfiguration)
  }
}
