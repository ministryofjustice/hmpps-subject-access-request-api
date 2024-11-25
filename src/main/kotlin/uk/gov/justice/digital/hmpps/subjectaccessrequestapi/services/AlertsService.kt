package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest

interface AlertsService {

  fun raiseOverdueReportAlert(overdueReports: List<SubjectAccessRequest?>)
}
