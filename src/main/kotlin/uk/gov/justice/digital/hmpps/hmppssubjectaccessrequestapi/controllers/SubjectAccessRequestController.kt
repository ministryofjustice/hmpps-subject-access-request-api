package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Report
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.ReportRepository
import java.time.LocalDateTime
import java.time.Month

@RestController
@RequestMapping("/api/")
class SubjectAccessRequestController() {

  @PostMapping("createSubjectAccessReport")
  fun createSubjectAccessReportPost(repo: ReportRepository): String {
    val dateFrom =
      LocalDateTime.of(2019, Month.MARCH, 28, 14, 33, 48)
    val dateTo =
      LocalDateTime.of(2020, Month.MARCH, 28, 14, 33, 48)
    val requestedDateTime =
      LocalDateTime.now()
    val claimDateTime =
      LocalDateTime.now()

    repo.save(
      Report(
        "14",
        "1",
        dateFrom,
        dateTo,
        "1",
        listOf("1", "2", "4"),
        "1",
        "1",
        "1",
        "1",
        "1",
        requestedDateTime,
        claimDateTime,
        "1",
        "1",
        1,
      ),
    )
    return "MockId" // Maybe want to return Report ID?
  }
}