package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.ServiceConfigurationNotFoundException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.utils.ServiceConfigurationComparator
import java.time.Instant
import java.util.UUID

@Service
class ServiceConfigurationService(private val serviceConfigurationRepository: ServiceConfigurationRepository) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  data class ServiceConfigurationUpdate(
    val id: UUID,
    val serviceName: String,
    val label: String,
    val url: String,
    val category: ServiceCategory,
    val enabled: Boolean,
    val templateMigrated: Boolean,
  )

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

  @Transactional
  fun deleteByServiceName(serviceName: String) = serviceConfigurationRepository.deleteByServiceName(serviceName)

  @Transactional
  fun updateServiceConfiguration(
    update: ServiceConfigurationUpdate,
  ): ServiceConfiguration {
    serviceConfigurationRepository.findByServiceNameAndIdNot(update.serviceName, update.id)?.let {
      throw serviceInUseValidationException(update.serviceName, it.id)
    }

    return serviceConfigurationRepository.findById(update.id)
      .orElseThrow { ServiceConfigurationNotFoundException(update.id) }
      .let { entity ->
        log.info("updating service configuration id: {}", entity.id)
        // Suspended field intentionally ignored and updated separately.
        entity.apply {
          serviceName = update.serviceName
          label = update.label
          url = update.url
          enabled = update.enabled
          templateMigrated = update.templateMigrated
          category = update.category
        }
        serviceConfigurationRepository.saveAndFlush(entity)
      }
  }

  @Transactional
  fun updateSuspended(
    id: UUID,
    suspended: Boolean,
  ): ServiceConfiguration = serviceConfigurationRepository.findByIdOrNull(id)?.let {
    log.info("updating service configuration id: {} suspended={}", id, suspended)
    it.suspended = suspended
    it.suspendedAt = if (suspended) Instant.now() else null

    serviceConfigurationRepository.saveAndFlush(it)
  } ?: throw ServiceConfigurationNotFoundException(id)

  private fun serviceInUseValidationException(
    serviceName: String,
    serviceConfigurationId: UUID,
  ) = ValidationException("serviceName '$serviceName' is already in use by Service configuration $serviceConfigurationId")
}
