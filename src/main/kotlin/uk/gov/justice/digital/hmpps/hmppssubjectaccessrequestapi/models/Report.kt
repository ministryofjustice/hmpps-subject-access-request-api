package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models

import java.time.LocalDateTime
data class Report(
  val id: String,
  val status: String,
  val dateFrom: LocalDateTime,
  val dateTo: LocalDateTime,
  val sarCaseReferenceNumber: String,
  val services: List<String>,
  val nomisId: String?,
  val ndeliusCaseReferenceId: String?,
  val hmppsId: String?,
  val subject: String,
  val requestedBy: String,
  val requestDateBySar: LocalDateTime,
  val claimDateTime: LocalDateTime,
  val objectURL: String?,
  val presignedURL: String?,
  val claimAttempts: Int
)