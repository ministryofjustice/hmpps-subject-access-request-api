package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class NomisMappingsClient(
  private val nomisMappingsApiWebClient: WebClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getNomisLocationMappings(dpsLocationIds: List<String>): List<NomisLocationMapping> = nomisMappingsApiWebClient.post()
    .uri("/api/locations/dps")
    .bodyValue(dpsLocationIds)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<NomisLocationMapping>>() {})
    .onErrorResume { error ->
      log.error("Error retrieving nomis location mappings", error)
      Mono.just(emptyList())
    }
    .block()!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NomisLocationMapping(
  val dpsLocationId: String,
  val nomisLocationId: Int,
)
