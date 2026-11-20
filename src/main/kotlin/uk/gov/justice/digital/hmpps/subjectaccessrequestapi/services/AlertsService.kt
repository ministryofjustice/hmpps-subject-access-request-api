package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AlertsService(
  val telemetryClient: TelemetryClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
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
}
