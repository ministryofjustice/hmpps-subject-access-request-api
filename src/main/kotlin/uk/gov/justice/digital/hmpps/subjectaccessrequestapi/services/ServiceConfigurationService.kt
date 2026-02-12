package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

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
}
