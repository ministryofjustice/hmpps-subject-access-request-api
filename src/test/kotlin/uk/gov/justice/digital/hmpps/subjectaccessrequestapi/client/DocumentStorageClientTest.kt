package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import java.util.*

class DocumentStorageClientTest {
  private val alerts: SlackNotificationService = mock()
  private val documentId = UUID.randomUUID()

  @Test
  fun `should alert and propagate an error while reading the download body`() {
    val failure = IllegalStateException("Download interrupted")
    val response = ClientResponse.create(HttpStatus.OK)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
      .body(Flux.error(failure))
      .build()

    val document = client(Mono.just(response)).retrieveDocument(documentId)

    verifyNoInteractions(alerts)
    val thrown = assertThrows<WebClientResponseException> { document!!.body!!.blockFirst() }
    assertThat(thrown.cause).isSameAs(failure)
    verify(alerts).sendDocumentApiCallFailedAlert("GET /documents/{id}/file", documentId)
  }

  @Test
  fun `should alert and propagate a delete failure without an HTTP response`() {
    val failure = IllegalStateException("Document API unavailable")
    val thrown = assertThrows<IllegalStateException> {
      client(Mono.error(failure)).deleteDocument(documentId)
    }

    assertThat(thrown).isSameAs(failure)
    verify(alerts).sendDocumentApiCallFailedAlert("DELETE /documents/{id}", documentId)
  }

  private fun client(response: Mono<ClientResponse>): DocumentStorageClient = DocumentStorageClient(
    WebClient.builder().baseUrl("http://document-api").exchangeFunction { response }.build(),
    alerts,
  )
}
