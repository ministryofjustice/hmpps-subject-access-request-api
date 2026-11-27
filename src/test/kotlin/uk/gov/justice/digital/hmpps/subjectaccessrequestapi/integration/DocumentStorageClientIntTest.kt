package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.WebClientConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiMockServer
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.io.File
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest(
  classes = [
    DocumentStorageClient::class,
    WebClientConfiguration::class,
    WebClientAutoConfiguration::class,
    OAuth2ClientAutoConfiguration::class,
    OAuth2TestConfig::class,
    SecurityAutoConfiguration::class,
  ],
)
@WithMockAuthUser
@MockitoBean(types = [SlackNotificationService::class])
class DocumentStorageClientIntTest(
  @Autowired private val documentStorageClient: DocumentStorageClient,
  @Autowired private val alerts: SlackNotificationService,
) {

  @Nested
  inner class GetDocument {

    @Test
    fun `should return report if found`() {
      val uuid = UUID.fromString("11111111-1111-1111-1111-111111111111")
      documentServiceApiMockServer.getDocument(uuid)

      val response = documentStorageClient.retrieveDocument(uuid)

      assertThat(response!!.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isNotNull

      val byteArray = response.body?.blockFirst()?.inputStream?.readAllBytes()
      val pdfBytes: ByteArray = File("dummy.pdf").inputStream().readAllBytes()
      assertThat(byteArray).isEqualTo(pdfBytes)
      verifyNoInteractions(alerts)
    }

    @Test
    fun `should return null if no report found`() {
      val uuid = UUID.fromString("11111111-1111-1111-1111-111111111112")

      val response = documentStorageClient.retrieveDocument(uuid)
      assertThat(response).isNull()
      verifyNoInteractions(alerts)
    }

    @Test
    fun `should send slack alert and propagate a 500 from the document storage api`() {
      val uuid = UUID.fromString("11111111-1111-1111-1111-111111111113")
      documentServiceApiMockServer.stubFor(get(urlEqualTo("/documents/$uuid/file")).willReturn(serverError()))

      assertThrows<WebClientResponseException.InternalServerError> {
        documentStorageClient.retrieveDocument(uuid)
      }
      verify(alerts).sendDocumentApiCallFailedAlert("GET /documents/{id}/file", uuid)
    }
  }

  @Nested
  inner class DeleteDocument {

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.DocumentStorageClientIntTest#deleteResponseStatuses")
    fun `should return the expected response status`(status: Int) {
      val documentId = UUID.randomUUID()
      documentServiceApiMockServer.deleteDocument(documentId, status)

      val response = documentStorageClient.deleteDocument(documentId)
      assertThat(response).isNotNull
      assertThat(response).isEqualTo(HttpStatus.valueOf(status))
      if (status == 204 || status == 404) {
        verifyNoInteractions(alerts)
      } else {
        verify(alerts).sendDocumentApiCallFailedAlert("DELETE /documents/{id}/purge", documentId)
      }
    }
  }

  companion object {
    @JvmField
    internal val documentServiceApiMockServer = DocumentServiceApiMockServer()

    @JvmField
    internal val hmppsAuthMockServer = HmppsAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      documentServiceApiMockServer.start()
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      documentServiceApiMockServer.stop()
      hmppsAuthMockServer.stop()
    }

    @JvmStatic
    fun deleteResponseStatuses() = listOf(204, 400, 401, 403, 404, 409, 500, 501, 502, 503, 504)
  }
}

class OAuth2TestConfig {
  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
    clientRegistrationRepository,
    authorizedClientService,
  )
}
