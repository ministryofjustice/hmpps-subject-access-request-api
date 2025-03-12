package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private const val SIZE = 200

@Component
class LocationsClient(
  private val locationsApiWebClient: WebClient,
) {

  fun getLocationDetails(page: Int): LocationResults = locationsApiWebClient.get()
    .uri("/locations", mapOf("page" to page, "size" to SIZE))
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<LocationResults>() {})
    .block()!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocationResults(
  val last: Boolean,
  val content: List<LocationDetails>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocationDetails(
  val id: String,
  val localName: String,
)
