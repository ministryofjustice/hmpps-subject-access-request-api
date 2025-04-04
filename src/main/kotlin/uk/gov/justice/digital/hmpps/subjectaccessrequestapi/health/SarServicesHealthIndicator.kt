package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.ServiceConfigurationService

@Service("sarServiceApis")
class SarServicesHealthIndicator(
  private val serviceConfigurationService: ServiceConfigurationService,
  private val dynamicServicesClient: DynamicServicesClient,
  @Value("\${G1-api.url}") private val g1ApiUrl: String,
  @Value("\${G2-api.url}") private val g2ApiUrl: String,
  @Value("\${G3-api.url}") private val g3ApiUrl: String,
  @Value("\${application.health.alt-services}") private val altHealthServices: List<String>,
  @Value("\${application.health.dev-portal.url}") private val devPortalUrl: String,
) : HealthIndicator {
  override fun health(): Health {
    val servicesHealth = serviceConfigurationService.getServiceConfigurationSanitised()
      ?.map {
        val serviceUrl = resolveUrlPlaceHolder(it)
        val serviceHealth =
          if (altHealthServices.contains(it.serviceName)) {
            dynamicServicesClient.getAlternativeServiceHealth(serviceUrl)
          } else {
            dynamicServicesClient.getServiceHealthPing(serviceUrl)
          }
        it.serviceName to serviceHealth.addExtraUrls(serviceUrl, it.serviceName, devPortalUrl)
          .restrictHealthInfo(it.serviceName)
      }?.toMap()
    return Health.up().withDetails(servicesHealth).build()
  }

  private fun resolveUrlPlaceHolder(serviceConfiguration: ServiceConfiguration): String = when (serviceConfiguration.serviceName) {
    "G1" -> g1ApiUrl
    "G2" -> g2ApiUrl
    "G3" -> g3ApiUrl
    else -> serviceConfiguration.url
  }

  private fun Health.restrictHealthInfo(serviceName: String): Health = when (serviceName) {
    "G1", "G2", "G3" -> Health.status(this.status).build()
    else -> this
  }
}
