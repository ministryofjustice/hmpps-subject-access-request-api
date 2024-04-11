package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.util.*

@Component
class DocumentStorageGateway(
  @Autowired val hmppsAuthGateway: HmppsAuthGateway,
  @Value("\${services.document-storage.base-url}") hmppsDocumentApiUrl: String,
) {
  private val webClient: WebClient = WebClient.builder().baseUrl(hmppsDocumentApiUrl).build()
  private val log = LoggerFactory.getLogger(this::class.java)

  fun retrieveDocument(documentId: UUID): ByteArrayInputStream? {
    val token = hmppsAuthGateway.getClientToken()
    val response = webClient.get().uri("/documents/" + documentId.toString() + "/file")
      .header("Authorization", "Bearer $token")
      .retrieve()
      .bodyToMono(ByteArray::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return if (response !== null) ByteArrayInputStream(response) else null
  }
}
