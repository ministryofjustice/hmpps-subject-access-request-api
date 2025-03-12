package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class NomisMappingsMockServer : WireMockServer(8086) {

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

  fun stubLocationMappings() {
    stubFor(
      post("/api/locations/dps")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """[
                    {
                        "dpsLocationId": "000047c0-38e1-482e-8bbc-07d4b5f57e23",
                        "nomisLocationId": 389406
                    },
                    {
                        "dpsLocationId": "00000be5-081c-4374-8214-18af310d3d4a",
                        "nomisLocationId": 80065
                    },
                    {
                        "dpsLocationId": "00041fb1-4710-476f-ada8-3ea7e8f2ae50",
                        "nomisLocationId": 167792
                    },
                    {
                        "dpsLocationId": "0004bd05-edb2-473b-bc39-f94c6ebe3b0b",
                        "nomisLocationId": 165542
                    }
                ]"""
                .trimIndent(),
            ),
        ),
    )
  }
}

class NomisMappingsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val nomisMappingsApi = NomisMappingsMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = nomisMappingsApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = nomisMappingsApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = nomisMappingsApi.stop()
}
