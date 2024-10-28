package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UserDetailsClient(
  private val nomisUserRolesApiWebClient: WebClient,
) {

  fun getNomisUserDetails(): List<UserDetails> =
    nomisUserRolesApiWebClient.get()
      .uri("/users/lastnames")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<UserDetails>>() {})
      .block()!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserDetails(
  val username: String,
  val lastName: String,
)
