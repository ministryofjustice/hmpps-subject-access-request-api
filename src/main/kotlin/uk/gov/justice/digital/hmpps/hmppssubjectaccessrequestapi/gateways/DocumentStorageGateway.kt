package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class DocumentStorageGateway(
  @Autowired val hmppsAuthGateway: HmppsAuthGateway,
  @Value("\${services.hmpps-document-storage.base-url}") hmppsDocumentApiUrl: String
) {
  private val webClient: WebClient = WebClient.builder().baseUrl(hmppsDocumentApiUrl).build()

  fun storeDocument(documentId: String, documentBody: String) {
    val token = hmppsAuthGateway.getClientToken()
    // TODO: Generate UUID from ID? Change our DB IDs to UUID v4s?
    // TODO: POST request to /documents/SUBJECT_ACCESS_REQUEST_REPORT/{UUID}
  }

  fun retrieveDocument(documentId: String) {
    val token = hmppsAuthGateway.getClientToken()
    // TODO: GET request to /documents/$documentId
  }
}
