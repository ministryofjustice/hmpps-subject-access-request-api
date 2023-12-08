package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.model.Report

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : JpaRepository<Report, Int> {

  @Query(
    value = "SELECT DISTINCT d.*" +
      "FROM report as d JOIN device_wearer as dw ON d.device_wearer_id = dw.id" +
      " WHERE dw.device_wearer_id = :deviceWearerId",

    nativeQuery = true,
  )

  fun findDevicesByDeviceWearerId(deviceWearerId: String): List<Report>?

  @Query(
    value = "SELECT d.*" +
      "FROM device as d" +
      " WHERE d.device_id = :deviceId",
    nativeQuery = true,
  )
  fun findDeviceByDeviceId(@Param("reportId") reportId: String): List<Report>?

  fun save(report: Report) {
    db.update(
      "Inserting into SAR table values ( ?, ? )",
      report.id, report.status
    )
  }
}
