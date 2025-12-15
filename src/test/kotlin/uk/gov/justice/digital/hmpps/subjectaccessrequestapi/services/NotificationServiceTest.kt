package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime

class NotificationServiceTest {
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val notifyConfiguration: NotifyConfiguration = mock()

  private val newTemplateVersionTemplateId = "new-template-version-template-id"
  private val emailAddress = "me@test.com"
  private val templateVersion = TemplateVersion(
    serviceConfiguration = ServiceConfiguration(
      serviceName = "service-one",
      label = "Service One",
      url = "http://my-service",
      order = 1,
      enabled = true,
      templateMigrated = true,
    ),
    version = 14,
    createdAt = LocalDateTime.parse("2025-11-16T10:15:30"),
  )

  private val notificationService =
    NotificationService(
      notificationClient,
      telemetryClient,
      authenticationFacade,
      notifyConfiguration,
    )

  @BeforeEach
  fun setup() {
    whenever(authenticationFacade.currentUsername).thenReturn("test_user")
    whenever(notifyConfiguration.newTemplateVersionTemplateId).thenReturn(newTemplateVersionTemplateId)
    whenever(notifyConfiguration.newTemplateVersionEmailAddresses).thenReturn(emailAddress)
  }

  @Test
  fun `Sends email notification`() {
    val expectedParameters = mapOf(
      "product" to "Service One",
      "version" to "14",
      "user" to "test_user",
      "datetime" to "16 November 2025 10:15:30",
    )

    notificationService.sendNewTemplateVersionNotification(templateVersion)

    verify(notificationClient).sendEmail(newTemplateVersionTemplateId, emailAddress, expectedParameters, null)
    verifyNoInteractions(telemetryClient)
  }

  @Test
  fun `Throws exception when error sending email notification`() {
    val expectedParameters = mapOf(
      "product" to "Service One",
      "version" to "14",
      "user" to "test_user",
      "datetime" to "16 November 2025 10:15:30",
    )
    doAnswer {
      throw NotificationClientException("Test message")
    }.whenever(notificationClient).sendEmail(newTemplateVersionTemplateId, emailAddress, expectedParameters, null)

    assertThatThrownBy { notificationService.sendNewTemplateVersionNotification(templateVersion) }
      .isInstanceOf(NotificationClientException::class.java)
      .hasMessage("Test message")

    verify(telemetryClient).trackEvent(
      "newTemplateVersionNotificationFailure",
      mapOf("product" to "Service One", "reason" to "NotificationClientException", "user" to "test_user"),
      null,
    )
  }

  @Test
  fun `Does not send email notification when email addresses not defined`() {
    whenever(notifyConfiguration.newTemplateVersionEmailAddresses).thenReturn("")

    notificationService.sendNewTemplateVersionNotification(templateVersion)

    verifyNoInteractions(notificationClient)
    verifyNoInteractions(telemetryClient)
  }
}
