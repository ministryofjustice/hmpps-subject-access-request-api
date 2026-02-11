package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.SlackApiException
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.HeaderBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.LoggerFactory
import java.io.IOException

interface SlackApiClient {

  @Throws(IOException::class, SlackApiException::class)
  fun chatPostMessage(req: ChatPostMessageRequest?): ChatPostMessageResponse
}

/**
 * Wrapper around the actual Slack [MethodsClient] that implements our [SlackApiClient] interface enabling us to easily
 * swapped to NoOp implementation when actual slack integration is not needed/wanted.
 */
class SlackMethodsClient(val client: MethodsClient) : SlackApiClient {
  override fun chatPostMessage(
    req: ChatPostMessageRequest?,
  ): ChatPostMessageResponse = this.client.chatPostMessage(req)
}

class NoOpSlackMethodsClient : SlackApiClient {
  private companion object {
    private val log = LoggerFactory.getLogger(NoOpSlackMethodsClient::class.java)
  }

  private var isOk: Boolean = true

  fun setResponseIsOk(isOk: Boolean) {
    this.isOk = isOk
  }

  override fun chatPostMessage(req: ChatPostMessageRequest?): ChatPostMessageResponse {
    log.info("NoOpSlackMethodsClient sending slack message to channel: ${req?.channel} \n${req.debugMessage()}")
    return NoOpSlackResponse(this.isOk)
  }

  class NoOpSlackResponse(var isOK: Boolean) : ChatPostMessageResponse() {
    override fun isOk(): Boolean = isOK

    override fun setOk(isOk: Boolean) {
      this.isOK = isOk
    }
  }
}

fun ChatPostMessageRequest?.debugMessage(): String {
  val body: MutableList<String> = mutableListOf()

  this?.blocks?.forEach { block ->
    when (block) {
      is DividerBlock -> body.add("-----")
      is HeaderBlock -> body.add("Header: ${block.text.text}")

      is SectionBlock -> {
        body.add("Section: ${block.text.text}")
        block.fields?.forEach { f -> body.add("\t\t-${f.text}") }
      }

      is ContextBlock -> {
        body.add("Context:")
        body.addAll(block.elements.filterIsInstance<MarkdownTextObject>().map { "\t-${it.text}" })
      }
    }
  }
  return "\nSlack Message:\n${body.joinToString("\n\t") { it }} \n"
}
