package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Report(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
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
    val requestDateBySar: LocalDateTime? = null,
    val claimDateTime: LocalDateTime? = null,
    val objectURL: String?,
    val presignedURL: String?,
    val claimAttempts: Int,


) {
  constructor() : this("", "", null, null, "", listOf("", ""),"", "", "", "",
  "",
    null, null, "", "", 0,)
}
