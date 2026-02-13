package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.util.UUID

class TemplateVersionIntTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var templateVersionRepository: TemplateVersionRepository

  @Autowired
  lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  val serviceName = "hmpps-example-service"

  var serviceConfig = ServiceConfiguration(
    id = UUID.fromString("953a5ece-334b-4797-bfb9-f0fa9ff48f7d"),
    serviceName = serviceName,
    label = "HMPPS Example Service",
    url = "http://localhost:8080/",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  @BeforeEach
  fun setup() {
    serviceConfigurationRepository.save(serviceConfig)
  }

  @AfterEach
  fun cleanup() {
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.deleteById(serviceConfig.id)
  }

  fun postTemplateVersion(
    id: UUID,
    templateBody: String,
    authRoles: List<String>? = listOf("ROLE_SAR_DATA_ACCESS"),
  ): WebTestClient.ResponseSpec = webTestClient
    .post()
    .uri("/api/templates/service/$id")
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .headers(setAuthorisation(roles = authRoles))
    .bodyValue(
      MultipartBodyBuilder().apply {
        part("file", getMultipartBodyPart(templateBody))
          .header(
            "Content-Disposition",
            "form-data; name=file; filename=test.txt",
          )
      }.build(),
    ).exchange()

  private fun getMultipartBodyPart(value: String) = object : ByteArrayResource(value.toByteArray()) {
    override fun getFilename(): String = "test.txt"
  }
}
