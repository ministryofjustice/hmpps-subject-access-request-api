package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.format.DateTimeFormatter

@Service
class NotificationService(
  private val notificationClient: NotificationClientApi,
  private val manageUsersApiClient: ManageUsersApiClient,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val notifyConfiguration: NotifyConfiguration,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dataTimeFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")
  }

  fun sendNewTemplateVersionNotification(templateVersion: TemplateVersion) {
    if (isNotBlank(notifyConfiguration.newTemplateVersionEmailAddresses)) {
      val product = templateVersion.serviceConfiguration?.label
      val user = authenticationFacade.currentUsername
      val parameters = mapOf(
        "product" to product,
        "version" to templateVersion.version.toString(),
        "user" to user?.let { manageUsersApiClient.getUserFullName(it) },
        "datetime" to templateVersion.createdAt.format(dataTimeFmt),
      )
      try {
        log.info("Sending new template version notification for product {}", product)
        notificationClient.sendEmail(
          notifyConfiguration.newTemplateVersionTemplateId,
          notifyConfiguration.newTemplateVersionEmailAddresses,
          parameters,
          null,
        )
      } catch (e: NotificationClientException) {
        val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
        log.warn("Failed to send new template version notification for product {}", product, e)
        telemetryClient.trackEvent(
          "newTemplateVersionNotificationFailure",
          mapOf("product" to product, "reason" to reason, "user" to user),
          null,
        )
        throw e
      }
    }
  }
}
