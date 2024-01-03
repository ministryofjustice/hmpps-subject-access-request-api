package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Report

@Repository
interface ReportRepository : JpaRepository<Report, Int> {
// save() is a built in method of JpaRepository. This method below would have extended that so isn't required
//  fun save(report: Report) {
//    db.update(
//      "Inserting into SAR table values ( ?, ? )",
//      report.id, report.status
//    )
//  }
}
