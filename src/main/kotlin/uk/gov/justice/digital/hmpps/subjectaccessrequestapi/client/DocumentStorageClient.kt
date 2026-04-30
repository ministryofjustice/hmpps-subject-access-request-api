package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class DocumentStorageClient(
  private val documentStorageWebClient: WebClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val SERVICE_NAME_HEADER = "Service-Name"
    private const val SERVICE_NAME_HEADER_VALUE = "DPS-Subject-Access-Requests"
  }

  fun retrieveDocument(documentId: UUID): ResponseEntity<Flux<InputStreamResource>>? {
    val response = documentStorageWebClient.get().uri("/documents/$documentId/file")
      .header(SERVICE_NAME_HEADER, SERVICE_NAME_HEADER_VALUE)
      .retrieve()
      .toEntityFlux(InputStreamResource::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    log.info("Response from document storage service $response.")
    return response
  }

  fun deleteDocument(documentId: UUID): HttpStatusCode? {
    val response = documentStorageWebClient.delete().uri("/documents/$documentId")
      .header(SERVICE_NAME_HEADER, SERVICE_NAME_HEADER_VALUE)
      .exchangeToMono { response -> Mono.just(response.statusCode())
      }.block()
    log.info("response status from document storage service: $response")
    return response
  }
}
