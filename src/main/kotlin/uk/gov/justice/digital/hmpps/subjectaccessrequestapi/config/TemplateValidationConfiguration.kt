package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateValidator

@Configuration
class TemplateValidationConfiguration {

  @Bean
  fun templateValidator(): TemplateValidator = TemplateValidator()
}