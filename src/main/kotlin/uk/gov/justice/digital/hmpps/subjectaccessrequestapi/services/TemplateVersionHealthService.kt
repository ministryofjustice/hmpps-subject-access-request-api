package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionHealthStatusRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class TemplateVersionHealthService(
  private val templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository,
  private val templateVersionService: TemplateVersionService,
  private val dynamicServicesClient: DynamicServicesClient,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
  @Value("\${application.alerts.template-health.unhealthy-threshold-minutes:30}") private val unhealthyStatusThreshold: Long,
  @Value("\${application.alerts.template-health.last-notified-threshold-minutes:120}") private val lastNotifiedThreshold: Long,
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
      templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfiguration.id)?.let {
        templateVersionHealthStatusRepository.updateStatusWhenChanged(
          serviceConfiguration.id,
          health,
          clock.instant(),
        ).also { updateCount ->
          updateCount.takeIf { updateCount > 0 }?.let {
            telemetryClient.trackHealthStatusChange(health, serviceConfiguration)
          }
        }
      } ?: run {
        templateVersionHealthStatusRepository.save(
          TemplateVersionHealthStatus(
            id = UUID.randomUUID(),
            serviceConfiguration = serviceConfiguration,
            status = health,
            lastModified = clock.instant(),
          ),
        ).let { telemetryClient.trackHealthStatusChange(health, serviceConfiguration) }
      }
      log.info("Updated template version health status in database for {}", serviceConfiguration.serviceName)
    } ?: run {
      log.info(
        "Could not update template version health status in database for {} as no template was found",
        serviceConfiguration.serviceName,
      )
    }
  }

  fun getUnhealthyTemplateVersionsMeetingNotificationCriteria() = templateVersionHealthStatusRepository.findUnhealthyTemplates(
    unhealthyStatusThreshold = Instant.now(clock).minusMinutes(unhealthyStatusThreshold),
    lastNotifiedThreshold = Instant.now(clock).minusMinutes(lastNotifiedThreshold),
  )

  @Transactional
  fun updateLastNotified(
    templateVersionHealthStatuses: List<TemplateVersionHealthStatus>,
    lastNotified: Instant,
  ) {
    templateVersionHealthStatuses.forEach { t -> t.lastNotified = lastNotified }
    templateVersionHealthStatusRepository.saveAllAndFlush(templateVersionHealthStatuses)
  }

  private fun Instant.minusMinutes(minutes: Long) = this.minus(
    minutes,
    ChronoUnit.MINUTES,
  )

  private fun getSha256HashValue(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun TelemetryClient.trackHealthStatusChange(
    newStatus: HealthStatusType,
    serviceConfiguration: ServiceConfiguration,
  ) {
    this.trackEvent(
      "templateVersionHealthStatusUpdated",
      mapOf(
        "newStatus" to newStatus.name,
        "serviceName" to serviceConfiguration.serviceName,
      ),
      null,
    )
  }
}
