package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.AUTH_TEST_TOKEN_VALUE

const val DYNAMIC_SERVICE_PORT = 8090

class DynamicServiceMockServer : WireMockServer(DYNAMIC_SERVICE_PORT) {

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

  fun stubGetTemplate(status: Int, body: String? = "<h1>Template one</h2>") {
    stubFor(
      get("/subject-access-request/template")
        .withHeader("Authorization", matching("Bearer $AUTH_TEST_TOKEN_VALUE"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "text/plain")
            .withBody(body)
            .withStatus(status),
        ),
    )
  }
}

class DynamicServiceExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val dynamicService = DynamicServiceMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = dynamicService.start()
  override fun beforeEach(context: ExtensionContext): Unit = dynamicService.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = dynamicService.stop()
}
