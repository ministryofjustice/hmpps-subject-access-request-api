package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
@Service
@Component
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  fun createEvent(who: String, what: String, detail: String) {
    if (who == "INTEGRATION_TEST_USER") {
      return
    }
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(
          objectMapper.writeValueAsString(
            HmppsAuditEvent(
              what = what,
              details = detail,
              who = who,
            ),
          ),
        )
        .build(),
    )
  }
}
