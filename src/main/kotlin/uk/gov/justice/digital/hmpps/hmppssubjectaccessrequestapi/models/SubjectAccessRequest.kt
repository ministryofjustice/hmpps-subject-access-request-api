package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime
enum class Status {
  Pending,
  Completed,
}

@Entity
data class SubjectAccessRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null,
  @Enumerated(EnumType.STRING)
  val status: Status = Status.Pending,
  val dateFrom: LocalDateTime? = null,
  val dateTo: LocalDateTime? = null,
  val sarCaseReferenceNumber: String,
  val services: String,
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
    null,
    Status.Pending,
    null,
    null,
    "",
    "",
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
