package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

data class ServiceSummary(val backlog: BacklogSummary?, val overdueReports: OverdueReportSummary?)

data class BacklogSummary(
  val count: Int = 0,
  val alertThreshold: Int,
  val alertFrequency: String?,
)

data class OverdueReportSummary(
  val count: Int = 0,
  val alertThreshold: String?,
  val alertFrequency: String?,
)