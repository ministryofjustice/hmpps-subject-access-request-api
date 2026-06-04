package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import java.util.concurrent.TimeUnit

@ConditionalOnExpression($$"${application.dev-tools.enabled:false}")
@RestController
@Transactional
@PreAuthorize("hasAnyRole('ROLE_SAR_ADMIN_ACCESS')")
@RequestMapping("/api/developer-tools")
class DeveloperToolsController(
  val slackNotificationService: SlackNotificationService
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val rateLimiter = RateLimiter.create(0.2) // 1 request every 5s
  }

  @PostMapping("/send-slack-diagnostic")
  fun sendTestSlackAlert(): ResponseEntity<String> {
    LOG.info("sending test slack diagnostic message")

    if (rateLimiter.tryAcquire(3, TimeUnit.SECONDS)) {
      LOG.info("sending test slack diagnostic message")
      slackNotificationService.sendDiagnosticMessage()
      return ResponseEntity(HttpStatus.OK)
    } else {
      LOG.info("failed to acquire rate-limiter for test slack diagnostic message")
      return ResponseEntity(HttpStatus.TOO_MANY_REQUESTS)
    }
  }
}