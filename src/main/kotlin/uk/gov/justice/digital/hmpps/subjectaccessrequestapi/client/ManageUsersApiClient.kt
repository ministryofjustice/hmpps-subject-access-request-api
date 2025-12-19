package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class ManageUsersApiClient(
  private val manageUsersApiWebClient: WebClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getUserFullName(username: String): String = manageUsersApiWebClient
    .get()
    .uri("/users/$username")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<GetUserDetailsResponse>() {})
    .onErrorResume { error ->
      log.error("Error retrieving user details, defaulting to username", error)
      Mono.empty()
    }
    .block()?.name ?: username

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class GetUserDetailsResponse(val name: String)
}
