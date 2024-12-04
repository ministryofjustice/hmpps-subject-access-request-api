package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

class SubjectAccessRequestBacklogThresholdException(message: String) : RuntimeException(message)

class SubjectAccessRequestTimeoutException(message: String, private val requestIds: List<String>) : RuntimeException(message) {
  override val message: String
    get() = "${super.message}: \n\nRequests:\n${requestIds.joinToString("\n")}"
}

class SubjectAccessRequestProcessingOverdueException(message: String) : RuntimeException(message)
