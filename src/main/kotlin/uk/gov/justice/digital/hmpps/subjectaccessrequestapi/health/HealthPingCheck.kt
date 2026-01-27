package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.health.contributor.Health
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.WebClientWrapper
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("hmppsAuth")
class HmppsAuthHealthPing(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

private const val DOCUMENT_STORE_NAME = "hmpps-document-management-api"
private const val PRISON_REGISTER_NAME = "prison-register"
private const val EXTERNAL_USERS_NAME = "hmpps-external-users-api"
private const val NOMIS_USER_ROLES_NAME = "nomis-user-roles-api"
private const val SAR_DELIUS_NAME = "subject-access-requests-and-delius"
private const val LOCATIONS_NAME = "hmpps-locations-inside-prison-api"
private const val NOMIS_MAPPING_NAME = "hmpps-nomis-mapping-service"
private const val MANAGE_USERS_NAME = "hmpps-manage-users-api"

@Component(DOCUMENT_STORE_NAME)
class DocumentStoreHealthPing(
  @Qualifier("documentStoreApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, DOCUMENT_STORE_NAME, portalUrl)

@Component(PRISON_REGISTER_NAME)
class PrisonRegisterHealthPing(
  @Qualifier("prisonRegisterWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, PRISON_REGISTER_NAME, portalUrl)

@Component(EXTERNAL_USERS_NAME)
class ExternalUserHealthPing(
  @Qualifier("externalUserApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, EXTERNAL_USERS_NAME, portalUrl)

@Component(NOMIS_USER_ROLES_NAME)
class NOMISHealthPing(
  @Qualifier("nomisUserRolesApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, NOMIS_USER_ROLES_NAME, portalUrl)

@Component(SAR_DELIUS_NAME)
class SarAndDeliusHealthPing(
  @Qualifier("sarAndDeliusApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, SAR_DELIUS_NAME, portalUrl)

@Component(LOCATIONS_NAME)
class LocationsApiHealthPing(
  @Qualifier("locationsApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, LOCATIONS_NAME, portalUrl)

@Component(NOMIS_MAPPING_NAME)
class NomisMappingsApiHealthPing(
  @Qualifier("nomisMappingsApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, NOMIS_MAPPING_NAME, portalUrl)

@Component(MANAGE_USERS_NAME)
class ManageUsersApiHealthPing(
  @Qualifier("manageUsersApiHealthWebClientWrapper") webClientWrapper: WebClientWrapper,
  @Value("\${application.health.dev-portal.url}") portalUrl: String,
) : ExtendedHealthPingCheck(webClientWrapper, MANAGE_USERS_NAME, portalUrl)

open class ExtendedHealthPingCheck(private val webClientWrapper: WebClientWrapper, private val serviceName: String, private val portalUrl: String) : HealthPingCheck(webClientWrapper.webClient) {
  override fun health(): Health = super.health().addExtraUrls(webClientWrapper.baseUrl, serviceName, portalUrl)
}

fun Health.addExtraUrls(serviceUrl: String, serviceName: String, portalUrl: String): Health {
  val amendedDetails = mutableMapOf<String, Any>()
  amendedDetails.putAll(this.details)
  amendedDetails.put("healthUrl", "$serviceUrl/health")
  amendedDetails.put("portalUrl", String.format(portalUrl, serviceName))
  return Health.status(this.status).withDetails(amendedDetails).build()
}
