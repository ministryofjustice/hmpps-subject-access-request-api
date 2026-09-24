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
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.RendererServiceFailureType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
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
  @param:Value("\${sentry.env:local}") private val environmentName: String,
  @param:Value("\${slack.bot.team-notifications-enabled.template-health:true}") private val templateHealthTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.template-registered:true}") private val templateRegisteredTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.service-suspended:true}") private val serviceSuspendedTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.service-unsuspended:true}") private val serviceUnsuspendedTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.reports-timed-out:true}") private val reportsTimedOutTeamNotificationsEnabled: Boolean,
  @param:Value("\${slack.bot.team-notifications-enabled.service-call-failed:true}") private val serviceCallFailedTeamNotificationsEnabled: Boolean,
  private val rendererServiceCallFailedAlertLimiter: RendererServiceCallFailedAlertLimiter,
  val slackApiClient: SlackApiClient,
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private const val SLACK_CONTEXT_TEXT_MAX_LENGTH = 3000
    private const val TRUNCATED_TEXT_SUFFIX = "..."
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

  fun sendRendererServiceCallFailedAlert(
    subjectAccessRequest: SubjectAccessRequest,
    requestServiceDetail: RequestServiceDetail,
    failureType: RendererServiceFailureType,
    statusCode: Int?,
    message: String?,
  ) {
    val recipients = recipientsFor(
      listOf(requestServiceDetail.serviceConfiguration.teamSlackChannelId),
      serviceCallFailedTeamNotificationsEnabled,
    )

    if (recipients.isEmpty()) {
      return
    }

    if (!rendererServiceCallFailedAlertLimiter.shouldSend(requestServiceDetail.serviceConfiguration.serviceName, failureType)) {
      LOG.info(
        "suppressing renderer service call failed slack alert for service={} failureType={}",
        requestServiceDetail.serviceConfiguration.serviceName,
        failureType,
      )
      return
    }

    sendMessage(
      recipients = recipients,
      blocks = buildRendererServiceCallFailedMessage(
        subjectAccessRequest = subjectAccessRequest,
        requestServiceDetail = requestServiceDetail,
        failureType = failureType,
        statusCode = statusCode,
        message = message,
      ),
      errorMessage = "error sending renderer service call failed slack alert",
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
            environmentContextText(),
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
          environmentContextText(),
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
          environmentContextText(),
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
          environmentContextText(),
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
            environmentContextText(),
          ),
        )
      },
    )
  }

  private fun buildRendererServiceCallFailedMessage(
    subjectAccessRequest: SubjectAccessRequest,
    requestServiceDetail: RequestServiceDetail,
    failureType: RendererServiceFailureType,
    statusCode: Int?,
    message: String?,
  ): List<LayoutBlock> {
    val fields = mutableListOf<TextObject>(
      markdownText("*SAR*"),
      markdownText("*Service*"),
      markdownText("*Failure type*"),
    ).apply {
      add(markdownText(subjectAccessRequest.sarCaseReferenceNumber))
      add(markdownText(requestServiceDetail.serviceConfiguration.serviceName))
      add(markdownText(failureType.displayName))
      statusCode?.let {
        add(markdownText("*Status code*"))
        add(markdownText(it.toString()))
      }
    }

    return asBlocks(
      header { it.text(plainText("Subject Access Request: Service Call Failed :warning:")) },
      section { s ->
        s.text(
          markdownText(
            "The HTML renderer failed to retrieve ${failureType.displayName} for *${requestServiceDetail.serviceConfiguration.label}*.",
          ),
        )
        s.fields(fields)
      },
      divider(),
      context { c ->
        c.elements(
          listOf(
            plainText(rendererServiceCallFailedContextMessage(message)),
            environmentContextText(),
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

  private fun environmentContextText() = markdownText("Environment: *$environmentName*")

  private fun timedOutServiceDetails(request: SubjectAccessRequest) = request.services
    .filter { it.renderStatus != RenderStatus.COMPLETE }

  private fun rendererServiceCallFailedContextMessage(message: String?): String {
    val text = message?.takeIf { it.isNotBlank() } ?: "No error message provided."

    return if (text.length <= SLACK_CONTEXT_TEXT_MAX_LENGTH) {
      text
    } else {
      text.take(SLACK_CONTEXT_TEXT_MAX_LENGTH - TRUNCATED_TEXT_SUFFIX.length) + TRUNCATED_TEXT_SUFFIX
    }
  }

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
