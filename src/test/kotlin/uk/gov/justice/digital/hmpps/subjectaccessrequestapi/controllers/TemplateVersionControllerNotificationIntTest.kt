package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.verify
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import uk.gov.service.notify.NotificationClientApi
import java.time.format.DateTimeFormatter

@TestPropertySource(
  properties = [
    "application.notify.new-template-version.email-addresses=myemail@test.com",
  ],
)
class TemplateVersionControllerNotificationIntTest : TemplateVersionIntTestBase() {

  @MockitoBean
  lateinit var notificationClient: NotificationClientApi

  private val templateV1Body = "HMPPS Example Service template version 1"
  private val templateV1Hash = "457c8112d022123fc1eee3743949bff01aab388e319314e1e792561d153a8db6"

  @Nested
  inner class PostNewTemplateVersionTestCases {

    @ParameterizedTest
    @CsvSource(
      value = [
        "ROLE_SAR_DATA_ACCESS",
        "ROLE_SAR_SUPPORT",
        "ROLE_SAR_REGISTER_TEMPLATE",
      ],
    )
    fun `post new template version sends notification and returns CREATED for valid role`(role: String) {
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
      val expectedParameters = mapOf(
        "product" to "HMPPS Example Service",
        "version" to "1",
        "user" to "AUTH_ADM",
        "datetime" to actual.createdAt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")),
      )
      verify(notificationClient).sendEmail("126055c3-fff0-469d-b62a-cf0f44c26618", "myemail@test.com", expectedParameters, null)
    }
  }
}
