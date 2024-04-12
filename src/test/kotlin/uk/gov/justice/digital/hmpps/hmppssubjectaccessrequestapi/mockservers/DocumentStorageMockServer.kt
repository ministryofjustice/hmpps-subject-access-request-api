package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import org.springframework.http.MediaType
import java.io.File
import java.io.InputStream

class DocumentStorageMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 4000
  }

  private val endpoints = mapOf(
    "report" to mapOf(
      "valid" to "/documents/11111111-2222-3333-4444-555555555555/file",
      "notFound" to "/documents/11111111-2222-3333-4444-666666666666/file",
      "serverError" to "/documents/11111111-2222-3333-4444-777777777777/file",
    ),
  )

  fun stubGetReport() {
    val inputStream: InputStream = File("dummy.pdf").inputStream()
    stubFor(
      get(endpoints["report"]?.get("valid"))
        .withHeader("Authorization", matching("Bearer ${HmppsAuthMockServer.TOKEN}"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE)
            .withStatus(200)
            .withBody(inputStream.readAllBytes()),
        ),
    )
    stubFor(
      get(endpoints["report"]?.get("notFound"))
        .withHeader("Authorization", matching("Bearer ${HmppsAuthMockServer.TOKEN}"))
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )
    stubFor(
      get(endpoints["report"]?.get("serverError"))
        .withHeader("Authorization", matching("Bearer ${HmppsAuthMockServer.TOKEN}"))
        .willReturn(
          aResponse()
            .withStatus(500),
        ),
    )
  }
}
