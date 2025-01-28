package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SarAndDeliusMockServer : WireMockServer(8083) {

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

  fun stubGetUserDetails() {
    stubFor(
      get("/user")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """[
                        {
                        "username": "AA46243",
                        "lastName": "SMITH"
                        },
                        {
                        "username": "ALI241",
                        "lastName": "JONES"
                        },
                        {
                        "username": "DB128Z",
                        "lastName": "ALI"
                        }
                        ]"""
                .trimIndent(),
            ),
        ),
    )
  }
}

class SarAndDeliusApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val sarAndDeliusApi = SarAndDeliusMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = sarAndDeliusApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = sarAndDeliusApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = sarAndDeliusApi.stop()
}
