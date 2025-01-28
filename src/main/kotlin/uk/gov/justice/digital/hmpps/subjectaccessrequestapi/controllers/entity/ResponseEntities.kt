package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.util.UUID

/**
 * Duplicate subject access request API response entity
 */
data class DuplicateRequestResponseEntity(
  val id: String,
  val originalId: String,
  val sarCaseReferenceNumber: String,
)

/**
 * Create subject access request API response entity
 */
data class CreateSubjectAccessRequestEntity(
  val nomisId: String? = null,

  val ndeliusId: String? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  val dateFrom: LocalDate? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  var dateTo: LocalDate? = null,

  val sarCaseReferenceNumber: String? = null,

  val services: String? = null,
) {

  companion object {
    fun from(subjectAccessRequest: SubjectAccessRequest): CreateSubjectAccessRequestEntity {
      return CreateSubjectAccessRequestEntity(
        nomisId = subjectAccessRequest.nomisId,
        ndeliusId = subjectAccessRequest.ndeliusCaseReferenceId,
        dateFrom = subjectAccessRequest.dateFrom,
        dateTo = subjectAccessRequest.dateTo,
        sarCaseReferenceNumber = subjectAccessRequest.sarCaseReferenceNumber,
        services = subjectAccessRequest.services,
      )
    }
  }
}

data class ServiceInfo(
  val id: UUID,
  val name: String,
  val label: String,
  val url: String,
  val order: Int,
)
