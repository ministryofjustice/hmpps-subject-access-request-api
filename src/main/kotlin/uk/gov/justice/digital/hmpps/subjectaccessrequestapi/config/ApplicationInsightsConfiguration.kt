package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * TelemetryClient gets altered at runtime by the java agent and so is a no-op otherwise
 */
@Configuration
class ApplicationInsightsConfiguration {
  @Bean
  fun telemetryClient(): TelemetryClient = TelemetryClient()
}

fun TelemetryClient.trackEvent(name: String, properties: Map<String, String>) = this.trackEvent(name, properties, null)

fun TelemetryClient.trackApiEvent(name: String, id: String, vararg kvpairs: Pair<String, String> = emptyArray()) {
  this.trackEvent(
    name,
    mapOf(
      "id" to id,
      *kvpairs,
    ),
    null,
  )
}
