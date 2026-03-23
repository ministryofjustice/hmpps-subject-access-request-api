package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientApi

@Configuration
class NotificationConfig {
  @Bean
  fun notificationClient(@Value("\${application.notify.key:invalidkey}") key: String): NotificationClientApi = NotificationClient(key)

  @Bean
  fun emailConfiguration(
    @Value("\${application.notify.new-template-version.template}") newTemplateVersionTemplateId: String,
    @Value("\${application.notify.new-template-version.email-addresses:}") newTemplateVersionEmailAddresses: String,
    @Value("\${application.notify.suspend-product.template}") suspendProductTemplateId: String,
    @Value("\${application.notify.suspend-product.email-addresses:}") suspendProductEmailAddresses: String,
    @Value("\${application.notify.unsuspend-product.template}") unsuspendProductTemplateId: String,
    @Value("\${application.notify.unsuspend-product.email-addresses:}") unsuspendProductEmailAddresses: String,
  ) = NotifyConfiguration(
    newTemplateVersion = NotifyConfigDetails(newTemplateVersionTemplateId, newTemplateVersionEmailAddresses),
    suspendProduct = NotifyConfigDetails(suspendProductTemplateId, suspendProductEmailAddresses),
    unsuspendProduct = NotifyConfigDetails(unsuspendProductTemplateId, unsuspendProductEmailAddresses),
  )
}

data class NotifyConfiguration(
  val newTemplateVersion: NotifyConfigDetails,
  val suspendProduct: NotifyConfigDetails,
  val unsuspendProduct: NotifyConfigDetails,
)

data class NotifyConfigDetails(
  val templateId: String,
  val emailAddresses: String,
)
