package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.trackEvent
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.SubjectAccessRequestTimeoutException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AlertsService(
  val telemetryClient: TelemetryClient,
  val alertConfig: AlertsConfiguration,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dataTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private const val REQUESTS_TIMED_OUT_MESSAGE =
      "Warning: %d requests updated to status 'Errored' after not completing within the processing threshold: '%s'"
  }

  fun raiseUnexpectedExceptionAlert(exception: Exception, properties: Map<String, String>? = null) {
    log.warn("unexpected error encountered", exception)

    telemetryClient.trackException(
      exception,
      properties,
      null,
    )

    Sentry.captureException(exception)
  }

  fun raiseReportsTimedOutAlert(timedOutRequests: List<SubjectAccessRequest?>) {
    val msg = REQUESTS_TIMED_OUT_MESSAGE.format(
      timedOutRequests.size,
      alertConfig.requestTimeoutAlertConfig.thresholdAsString(),
    )
    log.warn(msg)

    telemetryClient.trackEvent(
      "RequestsTimeoutAlert",
      mapOf(
        "backlog" to timedOutRequests.toString(),
        "threshold" to alertConfig.requestTimeoutAlertConfig.thresholdAsString(),
        "timestamp" to LocalDateTime.now().format(dataTimeFmt),
      ),
    )
    Sentry.captureException(SubjectAccessRequestTimeoutException(msg, timedOutRequests.map { it?.id.toString() }))
  }
}
