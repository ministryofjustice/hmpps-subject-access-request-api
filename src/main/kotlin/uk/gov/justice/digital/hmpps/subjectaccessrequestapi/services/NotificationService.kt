package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfigDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.NotifyConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.NotificationType.NEW_TEMPLATE_VERSION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.NotificationType.SUSPEND_PRODUCT
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.NotificationType.UNSUSPEND_PRODUCT
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter

@Service
class NotificationService(
  private val notificationClient: NotificationClientApi,
  private val manageUsersApiClient: ManageUsersApiClient,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val notifyConfiguration: NotifyConfiguration,
  private val clock: Clock,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dataTimeFmt = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm:ss")
  }

  fun sendNewTemplateVersionNotification(templateVersion: TemplateVersion) {
    sendNotification(
      NEW_TEMPLATE_VERSION,
      templateVersion.serviceConfiguration,
      notifyConfiguration.newTemplateVersion,
      mutableMapOf(
        "version" to templateVersion.version.toString(),
        "datetime" to templateVersion.createdAt.format(dataTimeFmt),
      ),
    )
  }

  fun sendSuspendProductNotification(serviceConfiguration: ServiceConfiguration) {
    sendNotification(
      SUSPEND_PRODUCT,
      serviceConfiguration,
      notifyConfiguration.suspendProduct,
      mutableMapOf(
        "datetime" to LocalDateTime.ofInstant(serviceConfiguration.suspendedAt, UTC).format(dataTimeFmt),
      ),
    )
  }

  fun sendUnsuspendProductNotification(serviceConfiguration: ServiceConfiguration) {
    sendNotification(
      UNSUSPEND_PRODUCT,
      serviceConfiguration,
      notifyConfiguration.unsuspendProduct,
      mutableMapOf(
        "datetime" to LocalDateTime.now(clock).format(dataTimeFmt),
      ),
    )
  }

  private fun sendNotification(
    notificationType: NotificationType,
    serviceConfiguration: ServiceConfiguration?,
    notifyDetails: NotifyConfigDetails,
    parameters: MutableMap<String, String?>,
  ) {
    if (isNotBlank(notifyDetails.emailAddresses)) {
      val product = serviceConfiguration?.label
      val user = authenticationFacade.currentUsername
      parameters["product"] = product
      parameters["user"] = user?.let { manageUsersApiClient.getUserFullName(it) }
      try {
        log.info("Sending {} notification for product {}", notificationType.label, product)
        notificationClient.sendEmail(
          notifyDetails.templateId,
          notifyDetails.emailAddresses,
          parameters,
          null,
        )
      } catch (e: NotificationClientException) {
        val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
        log.warn("Failed to send {} notification for product {}", notificationType.label, product, e)
        telemetryClient.trackEvent(
          notificationType.failureEventName,
          mapOf("product" to product, "reason" to reason, "user" to user),
          null,
        )
        throw e
      }
    }
  }
}

enum class NotificationType(val label: String, val failureEventName: String) {
  NEW_TEMPLATE_VERSION("new template version", "newTemplateVersionNotificationFailure"),
  SUSPEND_PRODUCT("suspend product", "suspendProductNotificationFailure"),
  UNSUSPEND_PRODUCT("unsuspend product", "unsuspendProductNotificationFailure"),
}
