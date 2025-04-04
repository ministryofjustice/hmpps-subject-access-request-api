package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck
import kotlin.jvm.java

@Service
class DynamicServicesClient(
  @Qualifier("dynamicHealthWebClient") private val dynamicHealthWebClient: WebClient,
) {
  fun getServiceHealthPing(serviceUrl: String): Health = ServiceHealthPingCheck(dynamicHealthWebClient, serviceUrl).health()

  fun getAlternativeServiceHealth(
    serviceUrl: String,
  ): Health = dynamicHealthWebClient.mutate().baseUrl(serviceUrl).build()
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
