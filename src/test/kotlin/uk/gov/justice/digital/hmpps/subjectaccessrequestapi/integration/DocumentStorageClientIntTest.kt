package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DocumentStorageClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.WebClientConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiMockServer
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthMockServer
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
class DocumentStorageClientIntTest {

  @Autowired
  private lateinit var documentStorageClient: DocumentStorageClient

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
  }

  @Test
  fun `should return null if no report found`() {
    val uuid = UUID.fromString("11111111-1111-1111-1111-111111111112")

    val response = documentStorageClient.retrieveDocument(uuid)
    assertThat(response).isNull()
  }

  @Test
  fun `should return null if 500 returned from document storage api`() {
    val uuid = UUID.fromString("11111111-1111-1111-1111-111111111113")

    val response = documentStorageClient.retrieveDocument(uuid)
    assertThat(response).isNull()
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
  }
}

class OAuth2TestConfig {
  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService)
}
