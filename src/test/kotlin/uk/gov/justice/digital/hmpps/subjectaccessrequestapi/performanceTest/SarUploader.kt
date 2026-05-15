package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.performanceTest

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.ServiceInfo
import java.io.File
import java.time.LocalDate

fun main(args: Array<String>) {
  SarUploader().loadRequests()
}

/**
 * Performance testing tool - Read the SAR Backlog request CSV and submit each row as a Subject Access Request.
 */
class SarUploader {

  private val webclient: WebClient = WebClient.create(System.getenv("SAR_ENDPOINT"))
  private val token: String = System.getenv("AUTH_TOKEN")
  private val csvPath = System.getenv("CSV_PATH")

  fun loadRequests() {
    val services = getServices()
    if (services.isNullOrEmpty()) {
      throw RuntimeException("No services found")
    }

    val sars = parseCsv(services)
    sars.forEach { createSAR(it)}
  }

  fun getServices(): List<String>? = webclient.get()
    .uri("/api/services")
    .header("Authorization", "bearer $token")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<ServiceInfo>>() {})
    .block()
    ?.filter { it.enabled }
    ?.map { it.name }

  fun parseCsv(
    services: List<String>,
  ): List<CreateSubjectAccessRequestEntity> = File(csvPath).bufferedReader()
    .readLines()
    .mapIndexed { index, row ->
      row.split(",")
        .takeIf { it.size == 13 }
        ?.let { r ->
          CreateSubjectAccessRequestEntity(
            nomisId = getIdOrNull(r[2]),
            ndeliusId = getIdOrNull(r[3]),
            dateFrom = LocalDate.parse(r[6]),
            dateTo = LocalDate.parse(r[7]),
            sarCaseReferenceNumber = "DL-" + r[1],
            services = services,
          )
        } ?: throw RuntimeException("CSV contained incorrect number of columns row $index")
    }

  fun createSAR(request: CreateSubjectAccessRequestEntity) {
    webclient.post()
      .uri("/api/subjectAccessRequest")
      .header("Authorization", "bearer $token")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(request)
      .retrieve()
      .onStatus({ t -> t.isError }, { it.createException() })
      .bodyToMono<String>()
      .block()
  }

  fun getIdOrNull(value: String?): String? = if (value.isNullOrEmpty() || value == "null") null else value
}