package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.MediaType
import java.io.File
import java.io.InputStream
import java.util.UUID

class DocumentServiceApiMockServer : WireMockServer(4040) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  fun getDocument(documentId: UUID) {
    val inputStream: InputStream = File("dummy.pdf").inputStream()
    stubFor(
      get(urlEqualTo("/documents/11111111-1111-1111-1111-111111111111/file"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE)
            .withHeader("Service-Name", "DPS Subject Access Requests")
            .withStatus(200)
            .withBody(inputStream.readAllBytes()),
        ),
    )

    stubFor(
      get(urlEqualTo("/documents/11111111-1111-1111-1111-111111111112/file"))
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )

    stubFor(
      get(urlEqualTo("/documents/11111111-1111-1111-1111-111111111113/file"))
        .willReturn(
          aResponse()
            .withStatus(500),
        ),
    )
  }

  fun deleteDocumentSuccess(id: UUID) {
    stubFor(
      delete(urlEqualTo("/documents/$id")).willReturn(aResponse().withStatus(204)),
    )
  }

  fun deleteDocumentError(id: UUID, status: Int) {
    stubFor(
      delete(urlEqualTo("/documents/$id")).willReturn(
        aResponse()
          .withStatus(status)
          .withBody(
            """
          {
            "status": $status,
            "errorCode": 666,
            "userMessage": "Error!!!!",
            "developerMessage": "SCARY ERROR!!!",
            "moreInfo": "SUPER SCARY UBER ERROR"
          }
            """.trimIndent(),
          ),
      ),
    )
  }
}

class DocumentServiceApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val documentServiceApi = DocumentServiceApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = documentServiceApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = documentServiceApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = documentServiceApi.stop()
}
