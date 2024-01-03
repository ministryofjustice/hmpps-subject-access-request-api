package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
data class SubjectAccessRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  val id: String,
  val status: String,
  val dateFrom: LocalDateTime? = null,
  val dateTo: LocalDateTime? = null,
  val sarCaseReferenceNumber: String,
  @ElementCollection
  val services: List<String>,
  val nomisId: String?,
  val ndeliusCaseReferenceId: String?,
  val hmppsId: String?,
  val subject: String,
  val requestedBy: String,
  val requestDateTime: LocalDateTime? = null,
  val claimDateTime: LocalDateTime? = null,
  val objectUrl: String?,
  val presignedUrl: String?,
  val claimAttempts: Int,
) {
  constructor() : this(
    "",
    "",
    null,
    null,
    "",
    listOf("", ""),
    "",
    "",
    "",
    "",
    "",
    null,
    null,
    "",
    "",
    0,
  )
}
