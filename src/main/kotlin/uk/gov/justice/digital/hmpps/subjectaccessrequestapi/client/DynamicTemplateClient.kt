package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration

@Service
class DynamicTemplateClient(
  @Qualifier("dynamicTemplateWebClient") private val templateWebClient: WebClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getServiceTemplate(serviceConfiguration: ServiceConfiguration): String? = templateWebClient
    .mutate().baseUrl(serviceConfiguration.url).build()
    .get()
    .uri("/subject-access-request/template")
    .exchangeToMono { resp ->
      if (resp.statusCode().is2xxSuccessful) {
        resp.bodyToMono(String::class.java)
      } else {
        resp.bodyToMono(String::class.java)
          .defaultIfEmpty("")
          .flatMap { _ ->
            log.error(
              "Problem retrieving template for {} - status {}",
              serviceConfiguration.serviceName,
              resp.statusCode(),
            )
            Mono.empty()
          }
      }
    }
    .doOnError { ex -> log.error("Problem retrieving template for {}", serviceConfiguration.serviceName, ex) }
    .onErrorResume { Mono.empty() }
    .block()
}
