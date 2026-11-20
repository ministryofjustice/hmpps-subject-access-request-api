package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

class SubjectAccessRequestTimeoutException(message: String, private val requestIds: List<String>) : RuntimeException(message) {
  override val message: String
    get() = "${super.message}: \n\nRequests:\n${requestIds.joinToString("\n")}"
}
