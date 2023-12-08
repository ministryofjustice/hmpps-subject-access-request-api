package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.model.Report
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.ReportRepository

interface IReportService {
  fun getAllReports(): List<Report>
  fun getDevicesByDeviceWearerId(deviceWearerId: String): List<Report>
  fun getDeviceByDeviceId(deviceId: String): Report?
}

@Service
class ReportService(@Autowired private val reportRepository: ReportRepository) :
  IReportService {
  override fun getAllReports(): List<Report> {
    return reportRepository.findAll().toList()
  }

  override fun getDevicesByDeviceWearerId(deviceWearerId: String): List<Report> {
    return reportRepository.findDevicesByDeviceWearerId(deviceWearerId) ?: listOf()
  }
  override fun getDeviceByDeviceId(deviceId: String): Report? {
    return reportRepository.findDeviceByDeviceId(deviceId)?.firstOrNull()
  }

}