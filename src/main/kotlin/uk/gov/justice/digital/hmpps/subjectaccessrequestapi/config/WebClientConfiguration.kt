package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${document-storage.url}") val documentStorageApiBaseUri: String,
  @param:Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${external-users-api.url}") val externalUserApiBaseUri: String,
  @param:Value("\${prison-register.url}") val prisonRegisterBaseUri: String,
  @param:Value("\${sar-and-delius-api.url}") val sarAndDeliusApiBaseUri: String,
  @param:Value("\${nomis-user-roles-api.url}") val nomisUserRolesApiBaseUri: String,
  @param:Value("\${locations-api.url}") val locationsApiBaseUri: String,
  @param:Value("\${nomis-mappings-api.url}") val nomisMappingsApiBaseUri: String,
  @param:Value("\${manage-users-api.url}") val manageUsersApiBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
  @param:Value("\${api.timeout:300s}") val longTimeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun documentStoreApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(documentStorageApiBaseUri, healthTimeout)

  @Bean
  fun documentStorageWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder
    .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024) }
    .authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = documentStorageApiBaseUri, longTimeout)

  @Bean
  fun prisonRegisterWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(prisonRegisterBaseUri, healthTimeout)

  @Bean
  fun externalUserApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(externalUserApiBaseUri, healthTimeout)

  @Bean
  fun externalUserApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = externalUserApiBaseUri, longTimeout)

  @Bean
  fun nomisUserRolesApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(nomisUserRolesApiBaseUri, healthTimeout)

  @Bean
  fun nomisUserRolesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = nomisUserRolesApiBaseUri, longTimeout)

  @Bean
  fun sarAndDeliusApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(sarAndDeliusApiBaseUri, healthTimeout)

  @Bean
  fun sarAndDeliusApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = sarAndDeliusApiBaseUri, longTimeout)

  @Bean
  fun locationsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", locationsApiBaseUri, longTimeout)

  @Bean
  fun locationsApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(locationsApiBaseUri, healthTimeout)

  @Bean
  fun nomisMappingsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", nomisMappingsApiBaseUri, longTimeout)

  @Bean
  fun nomisMappingsApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(nomisMappingsApiBaseUri, healthTimeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = manageUsersApiBaseUri, timeout)

  @Bean
  fun manageUsersApiHealthWebClientWrapper(builder: WebClient.Builder): WebClientWrapper = builder.wrappedHealthWebClient(manageUsersApiBaseUri, healthTimeout)

  @Bean
  fun dynamicHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient("", healthTimeout)
}

data class WebClientWrapper(val baseUrl: String, val webClient: WebClient)

fun WebClient.Builder.wrappedHealthWebClient(url: String, healthTimeout: Duration): WebClientWrapper = WebClientWrapper(url, this.healthWebClient(url, healthTimeout))
