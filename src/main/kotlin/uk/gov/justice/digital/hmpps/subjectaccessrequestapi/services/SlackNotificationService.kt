package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

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
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.SlackApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SlackNotificationService(
  @param:Value("\${slack.bot.dev-help-channel-id}") private val devHelpChannelId: String,
  @param:Value("\${slack.bot.template-error-recipients}") private val templateErrorRecipients: List<String>,
  @param:Value("\${slack.bot.team-notifications-enabled.template-health:true}") private val templateHealthTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.template-registered:true}") private val templateRegisteredTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.service-suspended:true}") private val serviceSuspendedTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.service-unsuspended:true}") private val serviceUnsuspendedTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.reports-timed-out:true}") private val reportsTimedOutTeamNotificationsEnabled: Boolean,
  val slackApiClient: SlackApiClient,
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
  }

  fun sendTemplateHealthAlert(unhealthyTemplates: List<TemplateVersionHealthStatus>) {
    sendMessage(
      recipients = recipientsFor(
        unhealthyTemplates.map { it.serviceConfiguration.teamSlackChannelId },
        templateHealthTeamNotificationsEnabled,
      ),
      blocks = buildTemplateHealthMessage(unhealthyTemplates),
      errorMessage = "error sending template health slack alert",
    )
  }

  fun sendNewTemplateVersionAlert(templateVersion: TemplateVersion) {
    sendMessage(
      recipients = recipientsFor(
        listOf(templateVersion.serviceConfiguration?.teamSlackChannelId),
        templateRegisteredTeamNotificationsEnabled,
      ),
      blocks = buildTemplateRegisteredMessage(templateVersion),
      errorMessage = "error sending template registered slack alert",
    )
  }

  fun sendSuspendProductAlert(serviceConfiguration: ServiceConfiguration) {
    sendMessage(
      recipients = recipientsFor(
        listOf(serviceConfiguration.teamSlackChannelId),
        serviceSuspendedTeamNotificationsEnabled,
      ),
      blocks = buildServiceSuspendedMessage(serviceConfiguration),
      errorMessage = "error sending service suspended slack alert",
    )
  }

  fun sendUnsuspendProductAlert(serviceConfiguration: ServiceConfiguration) {
    sendMessage(
      recipients = recipientsFor(
        listOf(serviceConfiguration.teamSlackChannelId),
        serviceUnsuspendedTeamNotificationsEnabled,
      ),
      blocks = buildServiceUnsuspendedMessage(serviceConfiguration),
      errorMessage = "error sending service unsuspended slack alert",
    )
  }

  fun sendReportsTimedOutAlert(timedOutRequests: List<SubjectAccessRequest>) {
    sendMessage(
      recipients = recipientsFor(
        timedOutRequests.flatMap { request ->
          timedOutServiceDetails(request).map { it.serviceConfiguration.teamSlackChannelId }
        },
        reportsTimedOutTeamNotificationsEnabled,
      ),
      blocks = buildReportsTimedOutMessage(timedOutRequests),
      errorMessage = "error sending reports timed out slack alert",
    )
  }

  private fun buildTemplateHealthMessage(
    unhealthyTemplates: List<TemplateVersionHealthStatus>,
  ): List<LayoutBlock> {
    val fields = mutableListOf<TextObject>(
      markdownText("*Service*"),
      markdownText("*First detected*"),
    )
    unhealthyTemplates.forEach { t ->
      fields.add(markdownText(t.serviceConfiguration.serviceName))
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

  private fun buildTemplateRegisteredMessage(templateVersion: TemplateVersion): List<LayoutBlock> = asBlocks(
    header { it.text(plainText("Subject Access Request: Template Registered :page_facing_up:")) },
    section { s ->
      s.text(
        markdownText(
          "A new template version has been registered for *${templateVersion.serviceConfiguration?.label ?: templateVersion.serviceConfiguration?.serviceName ?: "unknown service"}*.",
        ),
      )
    },
    divider(),
    context { c ->
      c.elements(
        listOf(
          markdownText("Version ${templateVersion.version} registered at ${templateVersion.createdAt.prettyFormat()}."),
        ),
      )
    },
  )

  private fun buildServiceSuspendedMessage(serviceConfiguration: ServiceConfiguration): List<LayoutBlock> = asBlocks(
    header { it.text(plainText("Subject Access Request: Service Suspended :double_vertical_bar:")) },
    section { s ->
      s.text(markdownText("Service *${serviceConfiguration.label}* has been suspended."))
    },
    divider(),
    context { c ->
      c.elements(
        listOf(
          markdownText("Reports for this service will remain suspended until it is unsuspended."),
        ),
      )
    },
  )

  private fun buildServiceUnsuspendedMessage(serviceConfiguration: ServiceConfiguration): List<LayoutBlock> = asBlocks(
    header { it.text(plainText("Subject Access Request: Service Unsuspended :arrow_forward:")) },
    section { s ->
      s.text(markdownText("Service *${serviceConfiguration.label}* has been unsuspended."))
    },
    divider(),
    context { c ->
      c.elements(
        listOf(
          markdownText("Reports for this service can now continue to be processed."),
        ),
      )
    },
  )

  private fun buildReportsTimedOutMessage(timedOutRequests: List<SubjectAccessRequest>): List<LayoutBlock> {
    val fields = mutableListOf<TextObject>(
      markdownText("*SAR*"),
      markdownText("*Service*"),
    )
    timedOutRequests.forEach { request ->
      fields.add(markdownText(request.sarCaseReferenceNumber))
      fields.add(markdownText(timedOutServiceDetails(request).map { it.serviceConfiguration.serviceName }.distinct().joinToString(", ")))
    }

    return asBlocks(
      header { it.text(plainText("Subject Access Request: Reports Timed Out :hourglass_flowing_sand:")) },
      section { s ->
        s.text(
          markdownText(
            "One or more Subject Access Requests did not complete within 48 hours and were marked as errored.",
          ),
        )
        s.fields(fields)
      },
      divider(),
      context { c ->
        c.elements(
          listOf(
            markdownText("Please investigate the failed requests and retry if necessary."),
          ),
        )
      },
    )
  }

  private fun recipientsFor(
    channelIds: Collection<String?>,
    teamNotificationsEnabled: Boolean,
  ): List<String> = (
    templateErrorRecipients +
      channelIds.takeIf { teamNotificationsEnabled }.orEmpty().filterNotNull()
    ).filter { it.isNotBlank() }
    .distinct()

  private fun timedOutServiceDetails(request: SubjectAccessRequest) = request.services
    .filter { it.renderStatus != RenderStatus.COMPLETE }

  private fun sendMessage(
    recipients: List<String>,
    blocks: List<LayoutBlock>,
    errorMessage: String,
  ) {
    if (recipients.isEmpty()) return

    recipients.forEach {
      val resp = slackApiClient.chatPostMessage(
        ChatPostMessageRequest.builder()
          .channel(it)
          .blocks(blocks)
          .build(),
      )

      if (!resp.isOk) {
        LOG.error("{}: {}", errorMessage, resp.error)
      }
    }
  }

  private fun Instant.prettyFormat(): String = this.atZone(ZoneId.of("UTC")).format(dateTimeFormatter)

  private fun LocalDateTime.prettyFormat(): String = this.atZone(ZoneId.of("UTC")).format(dateTimeFormatter)

  fun sendDiagnosticMessage() {
    val message = asBlocks(
      header { it.text(plainText("Subject Access Request: Test notification :hammer_and_wrench:")) },
      section { s ->
        s.text(
          markdownText("Test notification"),
        )
      },
      divider(),
      context { c ->
        c.elements(
          listOf(
            markdownText(
              "This is a test notification no action is required.",
            ),
          ),
        )
      },
    )
    templateErrorRecipients.takeIf { it.isNotEmpty() }?.forEach {
      val resp = slackApiClient.chatPostMessage(
        ChatPostMessageRequest.builder()
          .channel(it)
          .blocks(message)
          .build(),
      )

      if (!resp.isOk) {
        LOG.error("error sending test slack alert: {}", resp.error)
      }
    }
  }
}
