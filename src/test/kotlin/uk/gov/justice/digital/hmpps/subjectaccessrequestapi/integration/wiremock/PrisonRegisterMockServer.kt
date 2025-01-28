package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonRegisterMockServer : WireMockServer(9099) {

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

  fun stubGetPrisonDetails() {
    stubFor(
      get("/prisons/names")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """[
                        {
                        "prisonId": "AKI",
                        "prisonName": "Acklington (HMP)"
                        },
                        {
                        "prisonId": "ALI",
                        "prisonName": "Albany (HMP)"
                        },
                        {
                        "prisonId": "ANI",
                        "prisonName": "Aldington (HMP)"
                        }
                        ]"""
                .trimIndent(),
            ),
        ),
    )
  }
}

class PrisonRegisterApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonRegisterApi = PrisonRegisterMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = prisonRegisterApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = prisonRegisterApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = prisonRegisterApi.stop()
}
