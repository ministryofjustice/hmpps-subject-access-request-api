package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionHealthStatusRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDateTime

@Service
class TemplateVersionHealthService(
  private val templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository,
  private val templateVersionService: TemplateVersionService,
  private val dynamicServicesClient: DynamicServicesClient,
  private val clock: Clock,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updateTemplateVersionHealthData(serviceConfiguration: ServiceConfiguration) {
    log.info("Updating template version health status in database for {}", serviceConfiguration.serviceName)

    dynamicServicesClient.getServiceTemplate(serviceConfiguration)?.let { template ->
      val actualServiceHash = getSha256HashValue(template)
      val hashValid = templateVersionService.isTemplateHashValid(serviceConfiguration.id, actualServiceHash)
      val health = if (hashValid) HealthStatusType.HEALTHY else HealthStatusType.UNHEALTHY
      templateVersionHealthStatusRepository.updateStatusWhenChanged(
        serviceConfiguration.id,
        health,
        LocalDateTime.now(clock),
      )
      log.info("Updated template version health status in database for {}", serviceConfiguration.serviceName)
    } ?: run {
      log.info("Could not update template version health status in database for {} as no template was found", serviceConfiguration.serviceName)
    }
  }

  private fun getSha256HashValue(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
