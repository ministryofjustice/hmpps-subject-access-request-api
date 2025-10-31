package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.time.LocalDateTime
import java.util.UUID

class TemplateVersionControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var templateVersionRepository: TemplateVersionRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  private val serviceName = "hmpps-example-service"
  private val templateV1Body = "HMPPS Example Service template version 1"
  private val templateV1Hash = "457c8112d022123fc1eee3743949bff01aab388e319314e1e792561d153a8db6"
  private val templateV2Body = "HMPPS Example Service template version 2 - newer and better"
  private val templateV2Hash = "e54c30a7c4849a0c74e6a193528a267cf1851b90ea3bf79c4b4d149283749bab"

  private var serviceConfig = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = serviceName,
    label = "HMPPS Example Service",
    url = "http://localhost:8080/",
    order = 1,
    enabled = true,
    templateMigrated = true,
  )

  private val publishedTemplateV1 = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig,
    status = TemplateVersionStatus.PUBLISHED,
    version = 1,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v1-hash",
  )

  private val pendingTemplateV2 = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig,
    status = TemplateVersionStatus.PENDING,
    version = 2,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v2-hash",
  )

  @BeforeEach
  fun setup() {
    templateVersionRepository.deleteAll()

    serviceConfigurationRepository.deleteAll()
    serviceConfigurationRepository.save(serviceConfig)
  }

  @Nested
  inner class SecurityTestCases {

    @Test
    fun `get template endpoint returns UNAUTHORIZED when no auth header provided`() {
      webTestClient
        .get()
        .uri("/api/templates/${UUID.randomUUID()}")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get template endpoint returns FORBIDDEN when wrong auth header provided`() {
      webTestClient
        .get()
        .uri("/api/templates/service/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `post new template version returns UNAUTHORIZED when no auth header provided`() {
      postTemplateVersion(
        id = UUID.randomUUID(),
        templateBody = templateV1Body,
        authRoles = null,
      ).expectStatus().isUnauthorized
    }

    @Test
    fun `post new template version returns FORBIDEEN when the wrong auth header provided`() {
      postTemplateVersion(
        id = UUID.randomUUID(),
        templateBody = templateV1Body,
        authRoles = listOf("ROLE_WRONG"),
      ).expectStatus().isForbidden
    }
  }

  @Nested
  inner class GetTemplatesTestCases {

    @Test
    fun `get endpoint returns status 404 when the service configuration ID does not exist`() {
      webTestClient.get()
        .uri("/api/templates/service/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get endpoint returns empty list when no template versions exists for service`() {
      webTestClient.get()
        .uri("/api/templates/service/${serviceConfig.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `get template returns list of templates for service`() {
      templateVersionRepository.saveAll(listOf(publishedTemplateV1))

      webTestClient.get()
        .uri("/api/templates/service/${serviceConfig.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].id").isNotEmpty
        .jsonPath("$[0].serviceName").isEqualTo("hmpps-example-service")
        .jsonPath("$[0].version").isEqualTo(1)
        .jsonPath("$[0].createdDate").isEqualTo(publishedTemplateV1.createdAt)
        .jsonPath("$[0].fileHash").isEqualTo("template-v1-hash")
        .jsonPath("$[0].status").isEqualTo("PUBLISHED")
    }

    @Test
    fun `get template returns list of templates for service ordered by version desc`() {
      templateVersionRepository.save(publishedTemplateV1)
      templateVersionRepository.save(pendingTemplateV2)

      webTestClient.get()
        .uri("/api/templates/service/${serviceConfig.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)
        .jsonPath("$[0].id").isEqualTo(pendingTemplateV2.id)
        .jsonPath("$[0].serviceName").isEqualTo("hmpps-example-service")
        .jsonPath("$[0].version").isEqualTo(2)
        .jsonPath("$[0].createdDate").isEqualTo(pendingTemplateV2.createdAt)
        .jsonPath("$[0].fileHash").isEqualTo("template-v2-hash")
        .jsonPath("$[0].status").isEqualTo("PENDING")
        .jsonPath("$[1].id").isEqualTo(publishedTemplateV1.id)
        .jsonPath("$[1].serviceName").isEqualTo("hmpps-example-service")
        .jsonPath("$[1].version").isEqualTo(1)
        .jsonPath("$[1].createdDate").isEqualTo(publishedTemplateV1.createdAt)
        .jsonPath("$[1].fileHash").isEqualTo("template-v1-hash")
        .jsonPath("$[1].status").isEqualTo("PUBLISHED")
    }
  }

  @Nested
  inner class PostNewTemplateVersionTestCases {

    @Test
    fun `post new template returns status 404 when the service ID does not exist`() {
      postTemplateVersion(
        id = UUID.randomUUID(),
        templateBody = templateV1Body,
      ).expectStatus().isNotFound
    }

    @Test
    fun `post new template returns status 400 when template body is empty`() {
      postTemplateVersion(
        id = serviceConfig.id,
        templateBody = "",
      ).expectStatus().isBadRequest
    }

    @Test
    fun `post new template version returns CREATED`() {
      postTemplateVersion(id = serviceConfig.id, templateBody = templateV1Body)
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.id").isNotEmpty
        .jsonPath("$.serviceName").isEqualTo(serviceConfig.serviceName)
        .jsonPath("$.version").isEqualTo(1)
        .jsonPath("$.createdDate").isNotEmpty
        .jsonPath("$.fileHash").isEqualTo(templateV1Hash)
        .jsonPath("$.status").isEqualTo("PENDING")

      val actual = templateVersionRepository.findLatestByServiceConfigurationId(serviceConfig.id)
      assertThat(actual).isNotNull
      assertThat(actual!!.version).isEqualTo(1)
      assertThat(actual.serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(actual.fileHash).isEqualTo(templateV1Hash)
      assertThat(actual.status).isEqualTo(TemplateVersionStatus.PENDING)
    }

    @Test
    fun `post new template version deletes all existing template versions with status PENDING`() {
      templateVersionRepository.saveAll(listOf(publishedTemplateV1, pendingTemplateV2))

      var existingTemplateVersions =
        templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfig.id)

      assertThat(existingTemplateVersions).hasSize(2)
      assertThat(existingTemplateVersions[0]).isEqualTo(pendingTemplateV2)
      assertThat(existingTemplateVersions[1]).isEqualTo(publishedTemplateV1)

      val response = postTemplateVersion(serviceConfig.id, templateV2Body)
        .expectBody()
        .returnResult()
        .responseBody

      // Manual JSON deserialization required here as the Spring Web test client was losing precision unmarshalling the
      // LocalDateTime field.
      val mapper = ObjectMapper().registerModule(JavaTimeModule())
      val createdTemplate = mapper.readValue(response, TemplateVersionEntity::class.java)
      assertThat(createdTemplate).isNotNull

      existingTemplateVersions =
        templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfig.id)

      assertThat(existingTemplateVersions).hasSize(2)
      assertThat(existingTemplateVersions[0]).isEqualTo(
        TemplateVersion(
          id = createdTemplate!!.id!!,
          serviceConfiguration = serviceConfig,
          version = 2,
          createdAt = createdTemplate.createdDate!!,
          fileHash = templateV2Hash,
          status = TemplateVersionStatus.PENDING,
        ),
      )
      assertThat(existingTemplateVersions[1]).isEqualTo(publishedTemplateV1)
    }
  }

  private fun postTemplateVersion(
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
