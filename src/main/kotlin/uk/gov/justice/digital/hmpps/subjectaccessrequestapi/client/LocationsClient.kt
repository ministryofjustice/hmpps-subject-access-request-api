package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class LocationsClient(
  private val locationsApiWebClient: WebClient,
  @param:Value("\${application.locations-request.page-size}") private val pageSize: Int,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getLocationDetails(page: Int): LocationResults? = locationsApiWebClient.get()
    .uri { uriBuilder ->
      uriBuilder
        .path("/locations")
        .queryParam("page", page)
        .queryParam("size", pageSize)
        .build()
    }
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<LocationResults>() {})
    .onErrorResume { error ->
      log.error("Error retrieving locations for page {}", page, error)
      Mono.empty()
    }
    .block()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocationResults(
  val totalPages: Int,
  val content: List<LocationDetails>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocationDetails(
  val id: String,
  val localName: String?,
  val pathHierarchy: String,
)
