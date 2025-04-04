package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.WebClientWrapper

@Component
class PrisonRegisterClient(
  private val prisonRegisterWebClientWrapper: WebClientWrapper,
) {

  fun getPrisonDetails(): List<PrisonDetails> = prisonRegisterWebClientWrapper.webClient.get()
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
