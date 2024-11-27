package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class CreateSubjectAccessRequestEntity(
  val nomisId: String? = null,

  val ndeliusId: String? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  val dateFrom: LocalDate? = null,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  var dateTo: LocalDate? = null,

  val sarCaseReferenceNumber: String? = null,

  val services: String? = null,
)