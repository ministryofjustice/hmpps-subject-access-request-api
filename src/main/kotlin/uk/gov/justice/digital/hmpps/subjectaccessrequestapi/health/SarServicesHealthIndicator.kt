package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType.NOT_MIGRATED
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.TemplateVersionHealthService
import java.util.UUID

@Service("sarServiceApis")
class SarServicesHealthIndicator(
  private val serviceConfigurationService: ServiceConfigurationService,
  private val dynamicServicesClient: DynamicServicesClient,
  private val templateVersionHealthService: TemplateVersionHealthService,
  @param:Value("\${G1-api.url}") private val g1ApiUrl: String,
  @param:Value("\${G2-api.url}") private val g2ApiUrl: String,
  @param:Value("\${G3-api.url}") private val g3ApiUrl: String,
  @param:Value("\${application.health.alt-services}") private val altHealthServices: List<String>,
  @param:Value("\${application.health.dev-portal.url}") private val devPortalUrl: String,
) : HealthIndicator {
  override fun health(): Health {
    val serviceConfigs: List<ServiceConfiguration>? = serviceConfigurationService.getServiceConfigurationSanitised()

    val serviceIdToTemplateHealthStatus: Map<UUID, TemplateVersionHealthStatus> = serviceConfigs?.map { it.id }
      .let { templateVersionHealthService.getTemplateHealthStatusByServiceConfigurationIds(it) }

    val servicesHealth: MutableMap<String, Any> = serviceConfigs?.map {
      val serviceUrl = resolveUrlPlaceHolder(it)
      val templateHealthStatus = serviceIdToTemplateHealthStatus.getTemplateHealthStatusOrDefault(it.id)
      val serviceHealth =
        if (altHealthServices.contains(it.serviceName)) {
          dynamicServicesClient.getAlternativeServiceHealth(serviceUrl)
        } else {
          dynamicServicesClient.getServiceHealthPing(serviceUrl)
        }

      it.serviceName to serviceHealth.addExtraUrlsAndTemplateHealthStatus(
        serviceUrl,
        it.serviceName,
        devPortalUrl,
        templateHealthStatus,
      ).restrictHealthInfo(it.serviceName)
    }?.toMap()?.toMutableMap() ?: mutableMapOf()
    return Health.up().withDetails(servicesHealth).build()
  }

  private fun resolveUrlPlaceHolder(
    serviceConfiguration: ServiceConfiguration,
  ): String = when (serviceConfiguration.serviceName) {
    "G1" -> g1ApiUrl
    "G2" -> g2ApiUrl
    "G3" -> g3ApiUrl
    else -> serviceConfiguration.url
  }

  private fun Health.restrictHealthInfo(serviceName: String): Health = when (serviceName) {
    "G1", "G2", "G3" -> Health.status(this.status).build()
    else -> this
  }

  private fun Map<UUID, TemplateVersionHealthStatus>.getTemplateHealthStatusOrDefault(
    id: UUID,
  ): HealthStatusType = this[id]?.status ?: NOT_MIGRATED
}
