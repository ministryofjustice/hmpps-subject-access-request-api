package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NomisMappingsClient(
  private val nomisMappingsApiWebClient: WebClient,
) {

  fun getNomisLocationMappings(dpsLocationIds: List<String>): List<NomisLocationMapping> = nomisMappingsApiWebClient.post()
    .uri("/api/locations/dps")
    .bodyValue(dpsLocationIds)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<NomisLocationMapping>>() {})
    .block()!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NomisLocationMapping(
  val dpsLocationId: String,
  val nomisLocationId: Int,
)
