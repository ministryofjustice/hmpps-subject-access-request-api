package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import java.time.LocalDateTime
import java.util.UUID

class TemplateVersionControllerIntTest : TemplateVersionIntTestBase() {

  private val templateV1Body = "HMPPS Example Service template version 1"
  private val templateV1Hash = "457c8112d022123fc1eee3743949bff01aab388e319314e1e792561d153a8db6"
  private val templateV2Body = "HMPPS Example Service template version 2 - newer and better"
  private val templateV2Hash = "e54c30a7c4849a0c74e6a193528a267cf1851b90ea3bf79c4b4d149283749bab"

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

    @ParameterizedTest
    @CsvSource(
      value = [
        "ROLE_SAR_DATA_ACCESS",
        "ROLE_SAR_SUPPORT",
        "ROLE_SAR_REGISTER_TEMPLATE",
      ],
    )
    fun `get template returns list of templates for service for valid role`(role: String) {
      templateVersionRepository.saveAll(listOf(publishedTemplateV1))

      webTestClient.get()
        .uri("/api/templates/service/${serviceConfig.id}")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].id").isNotEmpty
        .jsonPath("$[0].serviceName").isEqualTo("hmpps-example-service")
        .jsonPath("$[0].version").isEqualTo(1)
        .jsonPath("$[0].createdDate").isNotEmpty
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
        .jsonPath("$[0].createdDate").isNotEmpty
        .jsonPath("$[0].fileHash").isEqualTo("template-v2-hash")
        .jsonPath("$[0].status").isEqualTo("PENDING")
        .jsonPath("$[1].id").isEqualTo(publishedTemplateV1.id)
        .jsonPath("$[1].serviceName").isEqualTo("hmpps-example-service")
        .jsonPath("$[1].version").isEqualTo(1)
        .jsonPath("$[1].createdDate").isNotEmpty
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

    @ParameterizedTest
    @CsvSource(
      value = [
        "ROLE_SAR_DATA_ACCESS",
        "ROLE_SAR_SUPPORT",
        "ROLE_SAR_REGISTER_TEMPLATE",
      ],
    )
    fun `post new template version returns CREATED for valid role`(role: String) {
      postTemplateVersion(id = serviceConfig.id, templateBody = templateV1Body, authRoles = listOf(role))
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
      assertThat(existingTemplateVersions[0].id).isEqualTo(pendingTemplateV2.id)
      assertThat(existingTemplateVersions[0].serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(existingTemplateVersions[0].version).isEqualTo(2)
      assertThat(existingTemplateVersions[0].createdAt).isNotNull()
      assertThat(existingTemplateVersions[0].fileHash).isEqualTo(pendingTemplateV2.fileHash)
      assertThat(existingTemplateVersions[0].status).isEqualTo(TemplateVersionStatus.PENDING)

      assertThat(existingTemplateVersions[1].id).isEqualTo(publishedTemplateV1.id)
      assertThat(existingTemplateVersions[1].serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(existingTemplateVersions[1].version).isEqualTo(1)
      assertThat(existingTemplateVersions[1].createdAt).isNotNull()
      assertThat(existingTemplateVersions[1].fileHash).isEqualTo(publishedTemplateV1.fileHash)
      assertThat(existingTemplateVersions[1].status).isEqualTo(TemplateVersionStatus.PUBLISHED)

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
      assertThat(existingTemplateVersions[0].id).isEqualTo(createdTemplate.id)
      assertThat(existingTemplateVersions[0].serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(existingTemplateVersions[0].version).isEqualTo(2)
      assertThat(existingTemplateVersions[0].createdAt).isNotNull()
      assertThat(existingTemplateVersions[0].fileHash).isEqualTo(templateV2Hash)
      assertThat(existingTemplateVersions[0].status).isEqualTo(TemplateVersionStatus.PENDING)

      assertThat(existingTemplateVersions[1].id).isEqualTo(publishedTemplateV1.id)
      assertThat(existingTemplateVersions[1].serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(existingTemplateVersions[1].version).isEqualTo(1)
      assertThat(existingTemplateVersions[1].createdAt).isNotNull()
      assertThat(existingTemplateVersions[1].fileHash).isEqualTo(publishedTemplateV1.fileHash)
      assertThat(existingTemplateVersions[1].status).isEqualTo(TemplateVersionStatus.PUBLISHED)
    }
  }
}
