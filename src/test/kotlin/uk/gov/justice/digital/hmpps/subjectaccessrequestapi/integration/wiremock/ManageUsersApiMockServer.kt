package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ManageUsersApiMockServer : WireMockServer(8087) {

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

  fun stubGetUserDetails(username: String) {
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                "username": "$username",
                "active": true,
                "name": "John Smith",
                "authSource": "nomis",
                "userId": "231232",
                "uuid": "5105a589-75b3-4ca0-9433-b96228c1c8f3"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetUserDetailsResponseFor(username: String, response: ResponseDefinitionBuilder) {
    stubFor(
      get("/users/$username")
        .willReturn(response),
    )
  }

  fun verifyGetUserDetailsApiCalled(username: String, times: Int = 1) = verify(
    times,
    getRequestedFor(urlPathEqualTo("/users/$username")),
  )
}

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsersApi = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = manageUsersApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = manageUsersApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = manageUsersApi.stop()
}
