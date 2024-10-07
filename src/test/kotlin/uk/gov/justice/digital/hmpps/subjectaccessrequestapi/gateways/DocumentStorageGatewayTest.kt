package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.gateways

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.mockservers.DocumentStorageMockServer
import java.io.File
import java.util.*

@ActiveProfiles("test")
@ContextConfiguration(
  initializers = [ConfigDataApplicationContextInitializer::class],
  classes = [(DocumentStorageGateway::class)],
)
class DocumentStorageGatewayTest(
  @MockBean val mockHmppsAuthGateway: HmppsAuthGateway,
) : DescribeSpec(
  {
    val documentStorageMockServer = DocumentStorageMockServer()

    Mockito.`when`(mockHmppsAuthGateway.getClientToken()).thenReturn("mock-bearer-token")

    beforeEach {
      documentStorageMockServer.start()
      documentStorageMockServer.stubGetReport()
    }

    afterTest {
      documentStorageMockServer.stop()
    }

    describe("getReport") {

      it("returns a report if one is returned from document storage service") {
        val response = DocumentStorageGateway(
          mockHmppsAuthGateway,
          documentStorageMockServer.baseUrl(),
        ).retrieveDocument(
          UUID.fromString("11111111-2222-3333-4444-555555555555"),
        )

        val byteArray = response?.body?.blockFirst()?.inputStream?.readAllBytes()
        val pdfBytes: ByteArray = File("dummy.pdf").inputStream().readAllBytes()
        byteArray.shouldBe(pdfBytes)
      }

      it("returns null if a 404 response is returned") {
        val response = DocumentStorageGateway(
          mockHmppsAuthGateway,
          documentStorageMockServer.baseUrl(),
        ).retrieveDocument(
          UUID.fromString("11111111-2222-3333-4444-666666666666"),
        )
        response.shouldBe(null)
      }

      it("throws an exception if another error response is returned") {
        shouldThrow<WebClientResponseException> {
          DocumentStorageGateway(
            mockHmppsAuthGateway,
            documentStorageMockServer.baseUrl(),
          ).retrieveDocument(
            UUID.fromString("11111111-2222-3333-4444-777777777777"),
          )
        }
      }
    }
  },
)
