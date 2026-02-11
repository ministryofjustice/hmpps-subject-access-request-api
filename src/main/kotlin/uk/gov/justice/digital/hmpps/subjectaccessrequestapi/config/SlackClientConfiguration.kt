package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import com.slack.api.Slack
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.NoOpSlackMethodsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.SlackApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.SlackMethodsClient

@Configuration
class SlackClientConfiguration {

  private companion object {
    private val log = LoggerFactory.getLogger(SlackClientConfiguration::class.java)
  }

  @Bean
  fun slackApiClient(
    @Value("\${slack.bot.noop-enabled:false}") noopEnabled: Boolean,
    @Value("\${slack.bot.auth}") slackBotAuthToken: String,
  ): SlackApiClient = if (noopEnabled) {
    log.info("initialising NoOpSlackMethodsClient NoOp Slack API integration")
    NoOpSlackMethodsClient()
  } else {
    log.info("initialising SlackMethodsClient for Slack API integration")
    SlackMethodsClient(Slack.getInstance().methods(slackBotAuthToken))
  }
}
