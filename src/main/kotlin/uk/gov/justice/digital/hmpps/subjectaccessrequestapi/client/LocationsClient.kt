package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class LocationsClient(
  private val locationsApiWebClient: WebClient,
  @Value("\${application.locations-request.page-size}") private val pageSize: Int,
) {

  fun getLocationDetails(page: Int): LocationResults = locationsApiWebClient.get()
    .uri("/locations", mapOf("page" to page, "size" to pageSize))
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
  val localName: String?,
  val pathHierarchy: String,
)
