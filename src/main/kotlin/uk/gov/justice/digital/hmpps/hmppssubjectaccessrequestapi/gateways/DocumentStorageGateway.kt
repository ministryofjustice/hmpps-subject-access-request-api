package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class DocumentStorageGateway(
  @Autowired val hmppsAuthGateway: HmppsAuthGateway,
  @Value("\${hmpps.document-storage.url}") hmppsDocumentApiUrl: String,
) {
  private val webClient: WebClient = WebClient.builder().baseUrl(hmppsDocumentApiUrl).build()

  fun storeDocument(documentId: String, documentBody: String) {
    val token = hmppsAuthGateway.getClientToken()
    webClient.post().uri("/documents/SUBJECT_ACCESS_REQUEST_REPORT" + { documentId }).header("Authorization", "Bearer $token").retrieve().bodyToMono(String::class.java).block()

    // TODO: Generate UUID from ID? Change our DB IDs to UUID v4s?
    // TODO: POST request to /documents/SUBJECT_ACCESS_REQUEST_REPORT/{UUID}
  }

  fun retrieveDocument(documentId: String): String? {
    val token = hmppsAuthGateway.getClientToken()
    return webClient.get().uri("/documents/" + { documentId }).header("Authorization", "Bearer $token").retrieve().bodyToMono(String::class.java).block()
  }
}
