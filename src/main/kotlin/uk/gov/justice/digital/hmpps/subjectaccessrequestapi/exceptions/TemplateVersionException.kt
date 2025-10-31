package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions

import java.util.UUID

open class TemplateVersionException(message: String) : RuntimeException(message)

class TemplateVersionServiceConfigurationNotFoundException(
  serviceConfigurationId: UUID,
) : TemplateVersionException(
  "create template version error service configuration id: $serviceConfigurationId not found",
)

class TemplateVersionTemplateBodyEmptyException(
  serviceConfigurationId: UUID,
) : TemplateVersionException(
  "create template version error for service: $serviceConfigurationId: template body was empty",
)
