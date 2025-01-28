package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PrisonRegisterClient(
  private val prisonRegisterWebClient: WebClient,
) {

  fun getPrisonDetails(): List<PrisonDetails> = prisonRegisterWebClient.get()
    .uri("/prisons/names")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<PrisonDetails>>() {})
    .block()!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonDetails(
  val prisonId: String,
  val prisonName: String,
)
