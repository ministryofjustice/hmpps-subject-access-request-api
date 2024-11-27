package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

import org.springframework.http.HttpStatus

open class SubjectAccessRequestException(message: String) : RuntimeException(message)

class CreateSubjectAccessRequestException(
  message: String,
  val status: HttpStatus,
) : SubjectAccessRequestException(message)
