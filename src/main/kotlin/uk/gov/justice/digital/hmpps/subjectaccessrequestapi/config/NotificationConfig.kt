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
  ) = NotifyConfiguration(newTemplateVersionTemplateId, newTemplateVersionEmailAddresses)
}

data class NotifyConfiguration(
  val newTemplateVersionTemplateId: String,
  val newTemplateVersionEmailAddresses: String,
)
