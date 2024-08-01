package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Component
class DocumentStorageGateway(
  @Autowired val hmppsAuthGateway: HmppsAuthGateway,
  @Value("\${services.document-storage.base-url}") hmppsDocumentApiUrl: String,
) {
  private val webClient: WebClient = WebClient.builder().baseUrl(hmppsDocumentApiUrl).build()
  private val log = LoggerFactory.getLogger(this::class.java)

  fun retrieveDocument(documentId: UUID): ResponseEntity<Flux<InputStreamResource>>? {
    val token = hmppsAuthGateway.getClientToken()

    val response = webClient.get().uri("/documents/$documentId/file")
      .header("Authorization", "Bearer $token")
      .header("Service-Name", "DPS-Subject-Access-Requests")
      .retrieve()
      .toEntityFlux(InputStreamResource::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    log.info("Response from document storage service $response.")
    return if (response !== null) response else null
  }

  fun deleteDocument(documentId: UUID): ResponseEntity<Flux<InputStreamResource>>? {
    val token = hmppsAuthGateway.getClientToken()

    val response = webClient.delete().uri("/documents/$documentId")
      .header("Authorization", "Bearer $token")
      .header("Service-Name", "DPS-Subject-Access-Requests")
      .retrieve()
      .toEntityFlux(InputStreamResource::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    log.info("Response from document storage service $response.")
    return if (response !== null) response else null
  }
}
