package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.HeaderBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.SlackApiClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant

class SlackNotificationServiceTest {

  private val slackClient: SlackApiClient = mock()
  private val chatPostMessageResponse: ChatPostMessageResponse = mock()

  private val serviceConfig: ServiceConfiguration = ServiceConfiguration(
    serviceName = "TestService",
    label = "Test",
    url = "http://localhost:8080",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val t1: TemplateVersionHealthStatus = TemplateVersionHealthStatus(
    serviceConfiguration = serviceConfig,
    status = HealthStatusType.UNHEALTHY,
    lastModified = Instant.parse("2026-01-22T14:30:00Z"),
  )

  private val slackNotificationService = SlackNotificationService(
    devHelpChannelId = "666",
    templateErrorRecipients = listOf("test-channel-01"),
    slackApiClient = slackClient,
  )

  @Test
  fun `should send expected Slack message`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)

    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendTemplateHealthAlert(listOf(t1))

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues).hasSize(1)

    val actual = messageCaptor.firstValue
    assertThat(actual.channel).isEqualTo("test-channel-01")
    assertThat(actual.blocks).hasSize(5)

    assertThat(actual.blocks[0]).isInstanceOf(HeaderBlock::class.java)
    assertThat((actual.blocks[0] as HeaderBlock).text).isNotNull
    assertThat((actual.blocks[0] as HeaderBlock).text.text)
      .isEqualTo("Subject Access Request: Template Health Check Failures :thermometer:")

    assertThat(actual.blocks[1]).isInstanceOf(SectionBlock::class.java)
    assertThat((actual.blocks[1] as SectionBlock).text).isNotNull
    assertThat((actual.blocks[1] as SectionBlock).text.text)
      .isEqualTo("One or more Subject Access Request Template Version Health Checks are failing")
    assertThat((actual.blocks[1] as SectionBlock).fields).hasSize(4)
    assertThat((actual.blocks[1] as SectionBlock).fields[0].text).isEqualTo("*Service*")
    assertThat((actual.blocks[1] as SectionBlock).fields[1].text).isEqualTo("*First detected*")
    assertThat((actual.blocks[1] as SectionBlock).fields[2].text).isEqualTo("TestService")
    assertThat((actual.blocks[1] as SectionBlock).fields[3].text).isEqualTo("22/01/2026 14:30:00")

    assertThat(actual.blocks[2]).isInstanceOf(SectionBlock::class.java)
    assertThat((actual.blocks[2] as SectionBlock).text).isNotNull
    assertThat((actual.blocks[2] as SectionBlock).text.text)
      .isEqualTo(":warning:  *All Subject Request Reports* requesting data from one of more of these services will be suspended until the template health issue is resolved. :warning:")

    assertThat(actual.blocks[3]).isInstanceOf(DividerBlock::class.java)

    assertThat(actual.blocks[4]).isInstanceOf(ContextBlock::class.java)
    assertThat((actual.blocks[4] as ContextBlock).elements).hasSize(1)
    assertThat(((actual.blocks[4] as ContextBlock).elements[0] as MarkdownTextObject).text)
      .isEqualTo("Please contact <#666> if you require guidance or assistance debugging this issue.")
  }
}
