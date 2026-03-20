package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.health.contributor.Health
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Service
class DynamicServicesClient(
  @Qualifier("dynamicHealthWebClient") private val dynamicWebClient: WebClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getServiceHealthPing(serviceUrl: String): Health = ServiceHealthPingCheck(dynamicWebClient, serviceUrl).health()

  fun getAlternativeServiceHealth(
    serviceUrl: String,
  ): Health = dynamicWebClient.mutate().baseUrl(serviceUrl).build()
    .get()
    .uri("/health")
    .retrieve()
    .toEntity(AlternativeHealth::class.java)
    .flatMap { Mono.just(it.body?.toHealth() ?: Health.down().build()) }
    .onErrorResume(WebClientResponseException::class.java) {
      Mono.just(
        Health.down(it).withDetail("body", it.responseBodyAsString).withDetail("HttpStatus", it.statusCode).build(),
      )
    }
    .onErrorResume(Exception::class.java) { Mono.just(Health.down(it).build()) }
    .block() ?: Health.down().build()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AlternativeHealth(
  val healthy: Boolean,
) {
  fun toHealth(): Health {
    if (healthy) {
      return Health.up().build()
    }
    return Health.down().build()
  }
}

class ServiceHealthPingCheck(webClient: WebClient, serviceUrl: String) : HealthPingCheck(webClient.mutate().baseUrl(serviceUrl).build())
