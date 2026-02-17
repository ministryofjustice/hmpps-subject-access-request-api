package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

import java.util.UUID

data class ServiceConfigurationNotFoundException(
  val id: UUID,
) : RuntimeException("Service configuration service not found for id: $id")
