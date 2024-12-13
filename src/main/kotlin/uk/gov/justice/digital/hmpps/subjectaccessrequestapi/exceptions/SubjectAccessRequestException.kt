package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

import org.springframework.http.HttpStatus

open class SubjectAccessRequestException(message: String) : RuntimeException(message)

/**
 * Parent Exception type for API exception that specify a HTTP status code to return.
 */
open class SubjectAccessRequestApiException(
  message: String,
  val status: HttpStatus,
  val subjectAccessRequestId: String? = null,
) : SubjectAccessRequestException(message)

class CreateSubjectAccessRequestException(
  message: String,
  status: HttpStatus,
  subjectAccessRequestId: String? = null,
) : SubjectAccessRequestApiException(message, status, subjectAccessRequestId)
