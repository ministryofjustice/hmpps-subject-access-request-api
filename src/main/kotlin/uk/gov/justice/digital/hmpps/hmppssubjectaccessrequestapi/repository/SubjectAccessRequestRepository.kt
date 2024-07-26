package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SubjectAccessRequestRepository : JpaRepository<SubjectAccessRequest, UUID> {
  @Query(
    "SELECT report FROM SubjectAccessRequest report " +
      "WHERE (report.status = 'Pending' " +
      "AND report.claimAttempts = 0) " +
      "OR (report.status = 'Pending' " +
      "AND report.claimAttempts > 0 " +
      "AND report.claimDateTime < :claimDateTime)",
  )
  fun findUnclaimed(@Param("claimDateTime") claimDateTime: LocalDateTime): List<SubjectAccessRequest?>

  fun findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch: String, nomisSearch: String, ndeliusSearch: String, pagination: Pageable): Page<SubjectAccessRequest?>

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE SubjectAccessRequest report " +
      "SET report.claimDateTime = :currentTime, report.claimAttempts = report.claimAttempts + 1" +
      "WHERE (report.id = :id AND report.claimDateTime < :releaseThreshold) OR (report.id = :id AND report.claimDateTime IS NULL)",
  )
  fun updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(@Param("id") id: UUID, @Param("releaseThreshold") releaseThreshold: LocalDateTime, @Param("currentTime") currentTime: LocalDateTime): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE SubjectAccessRequest report " +
      "SET report.status = :status " +
      "WHERE (report.id = :id)",
  )
  fun updateStatus(@Param("id") id: UUID, @Param("status") status: Status): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE SubjectAccessRequest report " +
      "SET report.lastDownloaded = :downloadDateTime " +
      "WHERE (report.id = :id)",
  )
  fun updateLastDownloaded(@Param("id") id: UUID, @Param("downloadDateTime") downloadDateTime: LocalDateTime): Int
}
