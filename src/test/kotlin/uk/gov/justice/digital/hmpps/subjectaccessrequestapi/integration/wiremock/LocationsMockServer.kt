package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LocationsMockServer : WireMockServer(8085) {

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

  fun stubGetLocationDetails(page: Int, totalPages: Int = 1) {
    stubFor(
      get(urlPathEqualTo("/locations"))
        .withQueryParam("page", equalTo(page.toString()))
        .withQueryParam("size", equalTo("50"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """{
                  "totalPages": $totalPages,
                  "content": [
                    {
                      "id": "00000be5-081c-4374-8214-18af310d3d4a",
                      "localName": "PROPERTY BOX 27",
                      "pathHierarchy": "PROP_BOXES-PB027"
                    },
                    {
                      "id": "000047c0-38e1-482e-8bbc-07d4b5f57e23",
                      "localName": "B WING",
                      "pathHierarchy": "RES-HB3-BWING"
                    },
                    {
                      "id": "00041fb1-4710-476f-ada8-3ea7e8f2ae50",
                      "pathHierarchy": "B-2-008"
                    },
                    {
                      "id": "0004bd05-edb2-473b-bc39-f94c6ebe3b0b",
                      "localName": "VALUABLES",
                      "pathHierarchy": "PROP_VALS"
                    }
                  ]
                }"""
                .trimIndent(),
            ),
        ),
    )
  }

  fun stubGetLocationDetailsFailure(page: Int) {
    stubFor(
      get(urlPathEqualTo("/locations"))
        .withQueryParam("page", equalTo(page.toString()))
        .withQueryParam("size", equalTo("50"))
        .willReturn(serviceUnavailable()),
    )
  }

  fun verifyGetLocationDetailsCalledForPage(page: Int) {
    verify(
      getRequestedFor(urlPathEqualTo("/locations"))
        .withQueryParam("page", equalTo(page.toString()))
        .withQueryParam("size", equalTo("50")),
    )
  }
}

class LocationsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val locationsApi = LocationsMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = locationsApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = locationsApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = locationsApi.stop()
}
