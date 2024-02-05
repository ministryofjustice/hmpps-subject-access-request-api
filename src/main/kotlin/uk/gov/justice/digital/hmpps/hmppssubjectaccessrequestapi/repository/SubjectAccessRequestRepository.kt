package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface SubjectAccessRequestRepository : JpaRepository<SubjectAccessRequest, Int> {
  fun findByClaimAttemptsIs(claimAttempts: Int): List<SubjectAccessRequest?>

  fun findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(status: Status, claimAttempts: Int, claimDateTime: LocalDateTime): List<SubjectAccessRequest?>

  @Modifying(clearAutomatically = true, flushAutomatically=true)
  @Query("UPDATE SubjectAccessRequest report " +
    "SET report.claimDateTime = :currentTime, report.claimAttempts = report.claimAttempts + 1" +
    "WHERE report.id = :id AND report.claimDateTime < :releaseThreshold")
  fun updateClaimDateTimeIfBeforeThreshold(@Param("id") id: Int, @Param("releaseThreshold") releaseThreshold: LocalDateTime, @Param("currentTime") currentTime: LocalDateTime): Int
}
