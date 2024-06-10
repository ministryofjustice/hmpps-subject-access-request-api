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
  fun findByStatusIsAndClaimAttemptsIs(status: Status, claimAttempts: Int): List<SubjectAccessRequest?>

  fun findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(status: Status, claimAttempts: Int, claimDateTime: LocalDateTime): List<SubjectAccessRequest?>

  @Query(
    "SELECT report FROM SubjectAccessRequest report " +
      "WHERE report.sarCaseReferenceNumber LIKE CONCAT('%', :search, '%') " +
      "OR report.nomisId LIKE CONCAT('%', :search, '%') " +
      "OR report.ndeliusCaseReferenceId LIKE CONCAT('%', :search, '%')",
  )
  fun findFilteredRecords(search: String): List<SubjectAccessRequest?>

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

//  fun findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch: String, nomisSearch: String, ndeliusSearch: String): List<SubjectAccessRequest?>

  fun findBySarCaseReferenceNumberContainingOrNomisIdContainingOrNdeliusCaseReferenceIdContaining(caseReferenceSearch: String, nomisSearch: String, ndeliusSearch: String, pagination: Pageable): Page<SubjectAccessRequest?>
}
