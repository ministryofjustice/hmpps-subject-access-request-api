package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

const val DYNAMIC_SERVICE_ALT_PORT = 8091

class DynamicServiceAltHealthMockServer : WireMockServer(DYNAMIC_SERVICE_ALT_PORT) {

  fun stubAltHealth(status: Int, body: String? = """{"healthy":${status == 200}}""") {
    stubFor(
      get("/health").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(body)
          .withStatus(status),
      ),
    )
  }
}

class DynamicServiceAltHealthExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val dynamicServiceAlt = DynamicServiceAltHealthMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = dynamicServiceAlt.start()
  override fun beforeEach(context: ExtensionContext): Unit = dynamicServiceAlt.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = dynamicServiceAlt.stop()
}
