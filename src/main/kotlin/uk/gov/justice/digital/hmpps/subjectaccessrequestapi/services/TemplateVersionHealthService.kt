package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicTemplateClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType.HEALTHY
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType.NOT_MIGRATED
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType.UNHEALTHY
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
  private val dynamicTemplateClient: DynamicTemplateClient,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
  @param:Value("\${application.alerts.template-health.unhealthy-threshold-minutes:30}") private val unhealthyStatusThreshold: Long,
  @param:Value("\${application.alerts.template-health.last-notified-threshold-minutes:120}") private val lastNotifiedThreshold: Long,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getTemplateHealthStatusByServiceConfigurationIds(
    serviceConfigurationIds: List<UUID>?,
  ): Map<UUID, TemplateVersionHealthStatus> = serviceConfigurationIds?.let {
    templateVersionHealthStatusRepository.findByServiceConfigurationIds(
      serviceConfigurationIds,
    ).associateBy { it.serviceConfiguration.id }
  } ?: emptyMap()

  @Transactional
  fun updateTemplateVersionHealthData(serviceConfiguration: ServiceConfiguration) {
    log.info("updating template version health status for {}", serviceConfiguration.serviceName)

    dynamicTemplateClient.getServiceTemplate(serviceConfiguration)?.let { template ->
      val actualServiceHash = getSha256HashValue(template)
      val hashValid = templateVersionService.isTemplateHashValid(
        serviceConfigurationId = serviceConfiguration.id,
        templateHash = actualServiceHash,
      )
      val health = if (hashValid) HEALTHY else UNHEALTHY

      templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfiguration.id)?.let {
        updateHealthStatusIfChanged(serviceConfiguration = serviceConfiguration, newHealthStatus = health)
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
      log.info(
        "updated template version health status service={}, healthy={}",
        serviceConfiguration.serviceName,
        health,
      )
    } ?: run {
      log.info(
        "could not update template version health status in database for {} as no template was found",
        serviceConfiguration.serviceName,
      )
    }
  }

  fun getUnhealthyTemplateVersionsMeetingNotificationCriteria() = templateVersionHealthStatusRepository.findUnhealthyTemplates(
    unhealthyStatusThreshold = Instant.now(clock).minusMinutes(unhealthyStatusThreshold),
    lastNotifiedThreshold = Instant.now(clock).minusMinutes(lastNotifiedThreshold),
  )

  private fun updateHealthStatusIfChanged(
    serviceConfiguration: ServiceConfiguration,
    newHealthStatus: HealthStatusType,
  ) {
    val updateCount = when (newHealthStatus) {
      HEALTHY -> {
        log.info("updating service {} template version health status to HEALTHY, ", serviceConfiguration.serviceName)

        templateVersionHealthStatusRepository.updateStatusToHealthyWhereUnhealthy(
          serviceConfigurationId = serviceConfiguration.id,
          currentTime = clock.instant(),
        )
      }

      UNHEALTHY -> {
        log.info("updating service {} template version health status to UNHEALTHY, ", serviceConfiguration.serviceName)

        templateVersionHealthStatusRepository.updateStatusToUnhealthyWhereHealthy(
          serviceConfigurationId = serviceConfiguration.id,
          currentTime = clock.instant(),
        )
      }

      NOT_MIGRATED -> 0
    }

    updateCount.takeIf { updateCount > 0 }?.let {
      telemetryClient.trackHealthStatusChange(newStatus = newHealthStatus, serviceConfiguration = serviceConfiguration)
    }
  }

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
