package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class NomisUserRolesMockServer : WireMockServer(8082) {

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
      get("/users/lastnames")
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

class NomisUserRolesApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val nomisUserRolesApi = NomisUserRolesMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = nomisUserRolesApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = nomisUserRolesApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = nomisUserRolesApi.stop()
}
