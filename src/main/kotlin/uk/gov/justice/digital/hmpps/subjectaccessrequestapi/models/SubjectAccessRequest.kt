package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

enum class Status {
  Pending,
  Completed,
  Errored,
}

@Entity
data class SubjectAccessRequest(
  @Id
  val id: UUID = UUID.randomUUID(),
  @Enumerated(EnumType.STRING)
  val status: Status = Status.Pending,
  val dateFrom: LocalDate? = null,
  var dateTo: LocalDate? = null,
  val sarCaseReferenceNumber: String = "",
  val services: String = "",
  val nomisId: String? = null,
  val ndeliusCaseReferenceId: String? = null,
  val requestedBy: String = "",
  val requestDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
  val claimDateTime: LocalDateTime? = null,
  val claimAttempts: Int = 0,
  val objectUrl: String? = null,
  val lastDownloaded: LocalDateTime? = null,
)
