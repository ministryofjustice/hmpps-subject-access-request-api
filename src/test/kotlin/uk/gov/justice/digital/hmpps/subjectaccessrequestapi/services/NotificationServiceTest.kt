package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfigDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class NotificationServiceTest {
  private val notificationClient: NotificationClientApi = mock()
  private val manageUsersApiClient: ManageUsersApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val newTemplateVersionTemplateId = "new-template-version-template-id"
  private val suspendTemplateId = "suspend-template-id"
  private val unsuspendTemplateId = "unsuspend-template-id"
  private val newTemplateVersionEmailAddress = "me@test.com"
  private val suspendEmailAddress = "me2@test.com"
  private val unsuspendEmailAddress = "me3@test.com"
  private val notifyConfiguration: NotifyConfiguration = spy(
    NotifyConfiguration(
      newTemplateVersion = NotifyConfigDetails(newTemplateVersionTemplateId, newTemplateVersionEmailAddress),
      suspendProduct = NotifyConfigDetails(suspendTemplateId, suspendEmailAddress),
      unsuspendProduct = NotifyConfigDetails(unsuspendTemplateId, unsuspendEmailAddress),
    ),
  )
  private val clock = Clock.fixed(LocalDateTime.parse("2026-02-09T14:33:23").toInstant(UTC), UTC)

  val serviceConfiguration = ServiceConfiguration(
    serviceName = "service-one",
    label = "Service One",
    url = "http://my-service",
    enabled = true,
    templateMigrated = true,
    category = PRISON,
    suspendedAt = LocalDateTime.parse("2026-03-22T16:06:56").toInstant(UTC),
  )
  private val templateVersion = TemplateVersion(
    serviceConfiguration = serviceConfiguration,
    version = 14,
    createdAt = LocalDateTime.parse("2025-11-16T10:15:30"),
  )

  private val notificationService =
    NotificationService(
      notificationClient,
      manageUsersApiClient,
      telemetryClient,
      authenticationFacade,
      notifyConfiguration,
      clock,
    )

  @BeforeEach
  fun setup() {
    whenever(authenticationFacade.currentUsername).thenReturn("test_user")
    whenever(manageUsersApiClient.getUserFullName("test_user")).thenReturn("John Smith")
  }

  @Nested
  inner class NewTemplateVersion {

    @Test
    fun `Sends email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "version" to "14",
        "user" to "John Smith",
        "datetime" to "16 November 2025 10:15:30",
      )

      notificationService.sendNewTemplateVersionNotification(templateVersion)

      verify(notificationClient).sendEmail(
        newTemplateVersionTemplateId,
        newTemplateVersionEmailAddress,
        expectedParameters,
        null,
      )
      verify(manageUsersApiClient).getUserFullName("test_user")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `Sends email notification when no username`() {
      whenever(authenticationFacade.currentUsername).thenReturn(null)
      val expectedParameters = mapOf(
        "product" to "Service One",
        "version" to "14",
        "user" to null,
        "datetime" to "16 November 2025 10:15:30",
      )

      notificationService.sendNewTemplateVersionNotification(templateVersion)

      verify(notificationClient).sendEmail(
        newTemplateVersionTemplateId,
        newTemplateVersionEmailAddress,
        expectedParameters,
        null,
      )
      verifyNoInteractions(manageUsersApiClient, telemetryClient)
    }

    @Test
    fun `Throws exception when error sending email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "version" to "14",
        "user" to "John Smith",
        "datetime" to "16 November 2025 10:15:30",
      )
      doAnswer {
        throw NotificationClientException("Test message")
      }.whenever(notificationClient)
        .sendEmail(newTemplateVersionTemplateId, newTemplateVersionEmailAddress, expectedParameters, null)

      assertThatThrownBy { notificationService.sendNewTemplateVersionNotification(templateVersion) }
        .isInstanceOf(NotificationClientException::class.java)
        .hasMessage("Test message")

      verify(manageUsersApiClient).getUserFullName("test_user")
      verify(telemetryClient).trackEvent(
        "newTemplateVersionNotificationFailure",
        mapOf("product" to "Service One", "reason" to "NotificationClientException", "user" to "test_user"),
        null,
      )
    }

    @Test
    fun `Does not send email notification when email addresses not defined`() {
      whenever(notifyConfiguration.newTemplateVersion).thenReturn(NotifyConfigDetails(newTemplateVersionTemplateId, ""))

      notificationService.sendNewTemplateVersionNotification(templateVersion)

      verifyNoInteractions(notificationClient, manageUsersApiClient, telemetryClient)
    }
  }

  @Nested
  inner class SuspendProduct {

    @Test
    fun `Sends email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to "John Smith",
        "datetime" to "22 March 2026 16:06:56",
      )

      notificationService.sendSuspendProductNotification(serviceConfiguration)

      verify(notificationClient).sendEmail(suspendTemplateId, suspendEmailAddress, expectedParameters, null)
      verify(manageUsersApiClient).getUserFullName("test_user")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `Sends email notification when no username`() {
      whenever(authenticationFacade.currentUsername).thenReturn(null)
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to null,
        "datetime" to "22 March 2026 16:06:56",
      )

      notificationService.sendSuspendProductNotification(serviceConfiguration)

      verify(notificationClient).sendEmail(suspendTemplateId, suspendEmailAddress, expectedParameters, null)
      verifyNoInteractions(manageUsersApiClient, telemetryClient)
    }

    @Test
    fun `Throws exception when error sending email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to "John Smith",
        "datetime" to "22 March 2026 16:06:56",
      )
      doAnswer {
        throw NotificationClientException("Test message")
      }.whenever(notificationClient)
        .sendEmail(suspendTemplateId, suspendEmailAddress, expectedParameters, null)

      assertThatThrownBy { notificationService.sendSuspendProductNotification(serviceConfiguration) }
        .isInstanceOf(NotificationClientException::class.java)
        .hasMessage("Test message")

      verify(manageUsersApiClient).getUserFullName("test_user")
      verify(telemetryClient).trackEvent(
        "suspendProductNotificationFailure",
        mapOf("product" to "Service One", "reason" to "NotificationClientException", "user" to "test_user"),
        null,
      )
    }

    @Test
    fun `Does not send email notification when email addresses not defined`() {
      whenever(notifyConfiguration.suspendProduct).thenReturn(NotifyConfigDetails(suspendTemplateId, ""))

      notificationService.sendSuspendProductNotification(serviceConfiguration)

      verifyNoInteractions(notificationClient, manageUsersApiClient, telemetryClient)
    }
  }

  @Nested
  inner class UnsuspendProduct {

    @Test
    fun `Sends email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to "John Smith",
        "datetime" to "9 February 2026 14:33:23",
      )

      notificationService.sendUnsuspendProductNotification(serviceConfiguration)

      verify(notificationClient).sendEmail(unsuspendTemplateId, unsuspendEmailAddress, expectedParameters, null)
      verify(manageUsersApiClient).getUserFullName("test_user")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `Sends email notification when no username`() {
      whenever(authenticationFacade.currentUsername).thenReturn(null)
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to null,
        "datetime" to "9 February 2026 14:33:23",
      )

      notificationService.sendUnsuspendProductNotification(serviceConfiguration)

      verify(notificationClient).sendEmail(unsuspendTemplateId, unsuspendEmailAddress, expectedParameters, null)
      verifyNoInteractions(manageUsersApiClient, telemetryClient)
    }

    @Test
    fun `Throws exception when error sending email notification`() {
      val expectedParameters = mapOf(
        "product" to "Service One",
        "user" to "John Smith",
        "datetime" to "9 February 2026 14:33:23",
      )
      doAnswer {
        throw NotificationClientException("Test message")
      }.whenever(notificationClient)
        .sendEmail(unsuspendTemplateId, unsuspendEmailAddress, expectedParameters, null)

      assertThatThrownBy { notificationService.sendUnsuspendProductNotification(serviceConfiguration) }
        .isInstanceOf(NotificationClientException::class.java)
        .hasMessage("Test message")

      verify(manageUsersApiClient).getUserFullName("test_user")
      verify(telemetryClient).trackEvent(
        "unsuspendProductNotificationFailure",
        mapOf("product" to "Service One", "reason" to "NotificationClientException", "user" to "test_user"),
        null,
      )
    }

    @Test
    fun `Does not send email notification when email addresses not defined`() {
      whenever(notifyConfiguration.unsuspendProduct).thenReturn(NotifyConfigDetails(unsuspendTemplateId, ""))

      notificationService.sendUnsuspendProductNotification(serviceConfiguration)

      verifyNoInteractions(notificationClient, manageUsersApiClient, telemetryClient)
    }
  }
}
