package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models

import java.time.LocalDateTime
data class Report(
  val id: String,
  val status: String,
  val date_from: LocalDateTime,
  val date_to: LocalDateTime,
  val sar_case_reference_number: String,
  val services: List<String>,
  val nomis_id: String?,
  val ndelius_case_reference_id: String?,
  val hmpps_id: String?,
  val subject: String,
  val requested_by: String,
  val request_date_by_sar: LocalDateTime,
  val claim_date_time: LocalDateTime,
  val object_url: String?,
  val presigned_url: String?,
  val claim_attempts: Int
)