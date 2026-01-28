package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackClientConfiguration(@param:Value("\${slack.bot.auth}") private val slackBotAuthToken: String) {

  @Bean
  fun slackClient(): MethodsClient = Slack.getInstance().methods(slackBotAuthToken)
}
