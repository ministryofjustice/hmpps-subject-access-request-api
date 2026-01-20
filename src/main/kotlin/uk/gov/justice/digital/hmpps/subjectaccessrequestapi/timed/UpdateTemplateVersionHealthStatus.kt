package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import io.sentry.Sentry
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionHealthStatusException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.TemplateVersionHealthService
import java.security.MessageDigest

/**
 * Update template version health status on a regular basis to enable the sending of alerts when mismatched for too long
 */
@Component
class UpdateTemplateVersionHealthStatus(private val service: UpdateTemplateVersionHealthService) {

  @Scheduled(
    fixedDelayString = $$"${application.template-version-health.frequency}",
    initialDelayString = $$"${random.int[60000,${application.template-version-health.frequency}]}",
  )
  @SchedulerLock(name = "updateTemplateVersionHealth", lockAtMostFor = $$"${application.template-version-health.lock-max}")
  fun updateTemplateVersionHealth() {
    try {
      service.updateTemplateVersionHealthData()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during template version health status update", e.javaClass.simpleName, e)
      Sentry.captureException(e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class UpdateTemplateVersionHealthService(
  private val serviceConfigurationRepository: ServiceConfigurationRepository,
  private val templateVersionHealthService: TemplateVersionHealthService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun updateTemplateVersionHealthData() {
    log.info("Updating template version health statuses in database")

    var failure = false
    serviceConfigurationRepository.findAllByEnabledAndTemplateMigrated()?.forEach { serviceConfiguration ->
      try {
        templateVersionHealthService.updateTemplateVersionHealthData(serviceConfiguration)
      } catch (e: Exception) {
        log.error("Problem updating template version health status for service {}", serviceConfiguration.serviceName, e)
        failure = true
      }
    }

    log.info("Template version health statuses updated in database")
    if (failure) {
      throw TemplateVersionHealthStatusException("At least one template version health status could not be updated")
    }
  }

  private fun getSha256HashValue(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
