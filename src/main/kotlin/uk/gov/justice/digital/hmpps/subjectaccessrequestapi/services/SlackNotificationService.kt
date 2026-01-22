package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.Blocks.asBlocks
import com.slack.api.model.block.Blocks.context
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.header
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.markdownText
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.TextObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SlackNotificationService(
  @Value("\${slack.bot.dev-help-channel-id}") private val devHelpChannelId: String,
  @Value("\${slack.bot.template-error-recipients}") private val templateErrorRecipients: List<String>,
  val slackClient: MethodsClient,
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
  }

  fun sendTemplateHealthAlert(unhealthyTemplates: List<TemplateVersionHealthStatus>) {
    templateErrorRecipients.takeIf { it.isNotEmpty() }?.forEach {
      val resp = slackClient.chatPostMessage(
        ChatPostMessageRequest.builder()
          .channel(it)
          .blocks(buildMessage(unhealthyTemplates))
          .build(),
      )

      if (!resp.isOk) {
        LOG.error("error sending template health slack alert: {}", resp.error)
      }
    }
  }

  private fun buildMessage(
    unhealthyTemplates: List<TemplateVersionHealthStatus>,
  ): List<LayoutBlock> {
    val fields = mutableListOf<TextObject>(
      markdownText("*Service*"),
      markdownText("*First detected*"),
    )
    unhealthyTemplates.forEach { t ->
      fields.add(markdownText(t.serviceConfiguration!!.serviceName))
      fields.add(markdownText(t.lastModified.prettyFormat()))
    }

    return asBlocks(
      header { it.text(plainText("Subject Access Request: Template Health Check Failures :thermometer:")) },
      section { s ->
        s.text(
          markdownText("One or more Subject Access Request Template Version Health Checks are failing"),
        )
        s.fields(fields)
      },
      section { s ->
        s.text(
          markdownText(
            ":warning:  *All Subject Request Reports* requesting data from one of more of these services will be suspended" +
              " until the template health issue is resolved. :warning:",
          ),
        )
      },
      divider(),
      context { c ->
        c.elements(
          listOf(
            markdownText(
              "Please contact <#$devHelpChannelId> if you require guidance or assistance debugging this issue.",
            ),
          ),
        )
      },
    )
  }

  private fun Instant.prettyFormat(): String = this.atZone(ZoneId.of("UTC")).format(dateTimeFormatter)
}
