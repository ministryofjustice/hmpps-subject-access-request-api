package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.toEntityFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import java.util.UUID

@Service
class DocumentStorageClient(
  private val documentStorageWebClient: WebClient,
  private val slackNotificationService: SlackNotificationService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val SERVICE_NAME_HEADER = "Service-Name"
    private const val SERVICE_NAME_HEADER_VALUE = "DPS-Subject-Access-Requests"
    private const val DOWNLOAD_PATH = "/documents/{id}/file"
    private const val DELETE_PATH = "/documents/{id}"
  }

  fun retrieveDocument(documentId: UUID): ResponseEntity<Flux<InputStreamResource>>? {
    val operation = "GET $DOWNLOAD_PATH"
    val response = try {
      documentStorageWebClient.get().uri(DOWNLOAD_PATH, documentId)
        .header(SERVICE_NAME_HEADER, SERVICE_NAME_HEADER_VALUE)
        .retrieve()
        .toEntityFlux<InputStreamResource>()
        .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
        .block()
    } catch (ex: RuntimeException) {
      slackNotificationService.sendDocumentApiCallFailedAlert(operation, documentId)
      throw ex
    }
    log.info("Response from document storage service $response.")
    return response?.let {
      ResponseEntity.status(it.statusCode).headers(it.headers).body(
        it.body?.onErrorResume { ex ->
          // Slack delivery blocks; keep it off the WebClient event loop.
          Mono.fromRunnable<Void> { slackNotificationService.sendDocumentApiCallFailedAlert(operation, documentId) }
            .subscribeOn(Schedulers.boundedElastic())
            .thenMany(Flux.error(ex))
        },
      )
    }
  }

  fun deleteDocument(documentId: UUID): HttpStatusCode? {
    val operation = "DELETE $DELETE_PATH"
    val response = try {
      documentStorageWebClient.delete().uri(DELETE_PATH, documentId)
        .header(SERVICE_NAME_HEADER, SERVICE_NAME_HEADER_VALUE)
        .exchangeToMono { response ->
          Mono.just(response.statusCode())
        }.block()
    } catch (ex: RuntimeException) {
      slackNotificationService.sendDocumentApiCallFailedAlert(operation, documentId)
      throw ex
    }
    if (response != HttpStatus.NO_CONTENT && response != HttpStatus.NOT_FOUND) {
      slackNotificationService.sendDocumentApiCallFailedAlert(operation, documentId)
    }
    log.info("response status from document storage service: $response")
    return response
  }
}
