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
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.RendererServiceFailureType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant

class SlackNotificationServiceTest {

  private val slackClient: SlackApiClient = mock()
  private val chatPostMessageResponse: ChatPostMessageResponse = mock()
  private val rendererServiceCallFailedAlertLimiter: RendererServiceCallFailedAlertLimiter = mock()

  private val serviceConfig: ServiceConfiguration = ServiceConfiguration(
    serviceName = "TestService",
    label = "Test",
    url = "http://localhost:8080",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
    teamSlackChannelId = "team-channel-01",
  )

  private val serviceConfigWithoutTeamChannel: ServiceConfiguration = ServiceConfiguration(
    serviceName = "OtherService",
    label = "Other",
    url = "http://localhost:8081",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val serviceConfigComplete: ServiceConfiguration = ServiceConfiguration(
    serviceName = "CompleteService",
    label = "Complete",
    url = "http://localhost:8082",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
    teamSlackChannelId = "team-channel-02",
  )

  private val t1: TemplateVersionHealthStatus = TemplateVersionHealthStatus(
    serviceConfiguration = serviceConfig,
    status = HealthStatusType.UNHEALTHY,
    lastModified = Instant.parse("2026-01-22T14:30:00Z"),
  )

  private val t2: TemplateVersionHealthStatus = TemplateVersionHealthStatus(
    serviceConfiguration = serviceConfigWithoutTeamChannel,
    status = HealthStatusType.UNHEALTHY,
    lastModified = Instant.parse("2026-01-22T15:45:00Z"),
  )

  private val templateVersion = TemplateVersion(
    serviceConfiguration = serviceConfig,
    version = 1,
  )

  private val timedOutSarBase = SubjectAccessRequest(
    sarCaseReferenceNumber = "SAR123",
    services = mutableListOf(),
  )

  private val timedOutSar = timedOutSarBase.addServices(
    RequestServiceDetail(
      subjectAccessRequest = timedOutSarBase,
      serviceConfiguration = serviceConfig,
      renderStatus = RenderStatus.PENDING,
    ),
  )

  private val timedOutSarWithCompletedServiceBase = SubjectAccessRequest(
    sarCaseReferenceNumber = "SAR456",
    services = mutableListOf(),
  )

  private val timedOutSarWithCompletedService = timedOutSarWithCompletedServiceBase.addServices(
    RequestServiceDetail(
      subjectAccessRequest = timedOutSarWithCompletedServiceBase,
      serviceConfiguration = serviceConfigComplete,
      renderStatus = RenderStatus.COMPLETE,
    ),
    RequestServiceDetail(
      subjectAccessRequest = timedOutSarWithCompletedServiceBase,
      serviceConfiguration = serviceConfig,
      renderStatus = RenderStatus.ERRORED,
    ),
  )

  private val slackNotificationService = createSlackNotificationService()

  private fun createSlackNotificationService(
    templateErrorRecipients: List<String> = listOf("test-channel-01"),
    templateHealthTeamNotificationsEnabled: Boolean = true,
    templateRegisteredTeamNotificationsEnabled: Boolean = true,
    serviceSuspendedTeamNotificationsEnabled: Boolean = true,
    serviceUnsuspendedTeamNotificationsEnabled: Boolean = true,
    reportsTimedOutTeamNotificationsEnabled: Boolean = true,
    serviceCallFailedTeamNotificationsEnabled: Boolean = true,
  ) = SlackNotificationService(
    devHelpChannelId = "666",
    templateErrorRecipients = templateErrorRecipients,
    templateHealthTeamNotificationsEnabled = templateHealthTeamNotificationsEnabled,
    templateRegisteredTeamNotificationsEnabled = templateRegisteredTeamNotificationsEnabled,
    serviceSuspendedTeamNotificationsEnabled = serviceSuspendedTeamNotificationsEnabled,
    serviceUnsuspendedTeamNotificationsEnabled = serviceUnsuspendedTeamNotificationsEnabled,
    reportsTimedOutTeamNotificationsEnabled = reportsTimedOutTeamNotificationsEnabled,
    serviceCallFailedTeamNotificationsEnabled = serviceCallFailedTeamNotificationsEnabled,
    rendererServiceCallFailedAlertLimiter = rendererServiceCallFailedAlertLimiter,
    slackApiClient = slackClient,
  )

  @Test
  fun `should send expected Slack message`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)

    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendTemplateHealthAlert(listOf(t1))

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues).hasSize(2)
    assertThat(messageCaptor.allValues.map { it.channel }).containsExactly(
      "test-channel-01",
      "team-channel-01",
    )

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

  @Test
  fun `should send alerts to global and team slack channels`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendTemplateHealthAlert(listOf(t1, t2))

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues.map { it.channel }).containsExactlyInAnyOrder(
      "test-channel-01",
      "team-channel-01",
    )
  }

  @Test
  fun `should send alerts to team slack channel when there are no global recipients`() {
    val service = createSlackNotificationService(templateErrorRecipients = emptyList())

    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    service.sendTemplateHealthAlert(listOf(t1))

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.firstValue.channel).isEqualTo("team-channel-01")
  }

  @Test
  fun `should send template registered alert to global and team slack channels`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendNewTemplateVersionAlert(templateVersion)

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues.map { it.channel }).containsExactlyInAnyOrder(
      "test-channel-01",
      "team-channel-01",
    )
  }

  @Test
  fun `should send timed out alert to global and team slack channels`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendReportsTimedOutAlert(listOf(timedOutSar))

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues.map { it.channel }).containsExactlyInAnyOrder(
      "test-channel-01",
      "team-channel-01",
    )
  }

  @Test
  fun `should only send timed out team slack alerts for non complete service details`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendReportsTimedOutAlert(listOf(timedOutSarWithCompletedService))

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues.map { it.channel }).containsExactlyInAnyOrder(
      "test-channel-01",
      "team-channel-01",
    )
    assertThat(messageCaptor.allValues.map { it.channel }).doesNotContain("team-channel-02")

    val actual = messageCaptor.firstValue
    assertThat((actual.blocks[1] as SectionBlock).fields[2].text).isEqualTo("SAR456")
    assertThat((actual.blocks[1] as SectionBlock).fields[3].text).isEqualTo("TestService")
  }

  @Test
  fun `should send only global slack channels when template health team notifications are disabled`() {
    val service = createSlackNotificationService(templateHealthTeamNotificationsEnabled = false)

    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    service.sendTemplateHealthAlert(listOf(t1))

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.firstValue.channel).isEqualTo("test-channel-01")
  }

  @Test
  fun `should not send team slack channels when template health notifications are disabled and no global recipients exist`() {
    val service = createSlackNotificationService(
      templateErrorRecipients = emptyList(),
      templateHealthTeamNotificationsEnabled = false,
    )

    service.sendTemplateHealthAlert(listOf(t1))

    verify(slackClient, times(0))
      .chatPostMessage(any<ChatPostMessageRequest>())
  }

  @Test
  fun `should send only global slack channels when template registered team notifications are disabled`() {
    val service = createSlackNotificationService(templateRegisteredTeamNotificationsEnabled = false)

    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    service.sendNewTemplateVersionAlert(templateVersion)

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.firstValue.channel).isEqualTo("test-channel-01")
  }

  @Test
  fun `should send only global slack channels when timed out team notifications are disabled`() {
    val service = createSlackNotificationService(reportsTimedOutTeamNotificationsEnabled = false)

    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    service.sendReportsTimedOutAlert(listOf(timedOutSar))

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.firstValue.channel).isEqualTo("test-channel-01")
  }

  @Test
  fun `should send renderer service call failed alert to global and team slack channels`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)
    whenever(rendererServiceCallFailedAlertLimiter.shouldSend(any(), any()))
      .thenReturn(true)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    slackNotificationService.sendRendererServiceCallFailedAlert(
      subjectAccessRequest = timedOutSar,
      requestServiceDetail = timedOutSar.services.first(),
      failureType = RendererServiceFailureType.SAR_DATA,
      statusCode = 500,
      message = "renderer failed after retries",
    )

    verify(slackClient, times(2))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.allValues.map { it.channel }).containsExactlyInAnyOrder(
      "test-channel-01",
      "team-channel-01",
    )
  }

  @Test
  fun `should send only global slack channels when service call failed team notifications are disabled`() {
    val service = createSlackNotificationService(serviceCallFailedTeamNotificationsEnabled = false)

    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)
    whenever(rendererServiceCallFailedAlertLimiter.shouldSend(any(), any()))
      .thenReturn(true)

    val messageCaptor = argumentCaptor<ChatPostMessageRequest>()

    service.sendRendererServiceCallFailedAlert(
      subjectAccessRequest = timedOutSar,
      requestServiceDetail = timedOutSar.services.first(),
      failureType = RendererServiceFailureType.SAR_DATA,
      statusCode = 500,
      message = "renderer failed after retries",
    )

    verify(slackClient, times(1))
      .chatPostMessage(messageCaptor.capture())

    assertThat(messageCaptor.firstValue.channel).isEqualTo("test-channel-01")
  }

  @Test
  fun `should suppress repeated renderer service call failed alerts within cooldown`() {
    whenever(chatPostMessageResponse.isOk).thenReturn(true)
    whenever(slackClient.chatPostMessage(any<ChatPostMessageRequest>()))
      .thenReturn(chatPostMessageResponse)
    whenever(rendererServiceCallFailedAlertLimiter.shouldSend(any(), any()))
      .thenReturn(true, false)

    slackNotificationService.sendRendererServiceCallFailedAlert(
      subjectAccessRequest = timedOutSar,
      requestServiceDetail = timedOutSar.services.first(),
      failureType = RendererServiceFailureType.SAR_DATA,
      statusCode = 500,
      message = "renderer failed after retries",
    )

    slackNotificationService.sendRendererServiceCallFailedAlert(
      subjectAccessRequest = timedOutSar,
      requestServiceDetail = timedOutSar.services.first(),
      failureType = RendererServiceFailureType.SAR_DATA,
      statusCode = 500,
      message = "renderer failed after retries",
    )

    verify(slackClient, times(2)).chatPostMessage(any<ChatPostMessageRequest>())
  }

  @Test
  fun `should not send renderer service call failed alert when limiter suppresses it`() {
    whenever(rendererServiceCallFailedAlertLimiter.shouldSend(any(), any()))
      .thenReturn(false)

    slackNotificationService.sendRendererServiceCallFailedAlert(
      subjectAccessRequest = timedOutSar,
      requestServiceDetail = timedOutSar.services.first(),
      failureType = RendererServiceFailureType.SAR_DATA,
      statusCode = 500,
      message = "renderer failed after retries",
    )

    verify(slackClient, times(0)).chatPostMessage(any<ChatPostMessageRequest>())
  }
}
