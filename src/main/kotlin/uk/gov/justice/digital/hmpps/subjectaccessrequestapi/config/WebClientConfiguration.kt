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
  @Value("\${document-storage.url}") val documentStorageApiBaseUri: String,
  @Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @Value("\${external-users-api.url}") val externalUserApiBaseUri: String,
  @Value("\${prison-register.url}") val prisonRegisterBaseUri: String,
  @Value("\${sar-and-delius-api.url}") val sarAndDeliusApiBaseUri: String,
  @Value("\${nomis-user-roles-api.url}") val nomisUserRolesApiBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:20s}") val timeout: Duration,
  @Value("\${api.timeout:300s}") val longTimeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun documentStoreApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(documentStorageApiBaseUri, healthTimeout)

  @Bean
  fun documentStorageWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder
    .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024) }
    .authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = documentStorageApiBaseUri, longTimeout)

  @Bean
  fun prisonRegisterWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonRegisterBaseUri, healthTimeout)

  @Bean
  fun externalUserApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(externalUserApiBaseUri, healthTimeout)

  @Bean
  fun externalUserApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = externalUserApiBaseUri, longTimeout)

  @Bean
  fun nomisUserRolesApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(nomisUserRolesApiBaseUri, healthTimeout)

  @Bean
  fun nomisUserRolesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = nomisUserRolesApiBaseUri, longTimeout)

  @Bean
  fun sarAndDeliusApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(sarAndDeliusApiBaseUri, healthTimeout)

  @Bean
  fun sarAndDeliusApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sar-client", url = sarAndDeliusApiBaseUri, longTimeout)
}
