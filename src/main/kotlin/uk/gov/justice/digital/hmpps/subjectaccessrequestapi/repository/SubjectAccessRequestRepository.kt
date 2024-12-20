package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

const val LOCK_TIMEOUT = "3000"

@Repository
interface SubjectAccessRequestRepository : JpaRepository<SubjectAccessRequest, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LOCK_TIMEOUT)])
  override fun findById(id: UUID): Optional<SubjectAccessRequest>

  @Lock(LockModeType.PESSIMISTIC_READ)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LOCK_TIMEOUT)])
  @Query(
    "SELECT report FROM SubjectAccessRequest report " +
      "WHERE (report.status = 'Pending' " +
      "AND report.claimAttempts = 0) " +
      "OR (report.status = 'Pending' " +
      "AND report.claimAttempts > 0 " +
      "AND report.claimDateTime < :claimDateTime)",
  )
  fun findUnclaimed(@Param("claimDateTime") claimDateTime: LocalDateTime): List<SubjectAccessRequest?>

  fun findBySarCaseReferenceNumberContainingIgnoreCaseOrNomisIdContainingIgnoreCaseOrNdeliusCaseReferenceIdContainingIgnoreCase(
    caseReferenceSearch: String,
    nomisSearch: String,
    ndeliusSearch: String,
    pagination: Pageable,
  ): Page<SubjectAccessRequest?>

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE SubjectAccessRequest report " +
      "SET report.claimDateTime = :currentTime, report.claimAttempts = report.claimAttempts + 1" +
      "WHERE (report.status = 'Pending' AND report.id = :id AND report.claimDateTime < :releaseThreshold) " +
      "OR (report.status = 'Pending' AND report.id = :id AND report.claimDateTime IS NULL)",
  )
  fun updateClaimDateTimeAndClaimAttemptsIfBeforeThreshold(
    @Param("id") id: UUID,
    @Param("releaseThreshold") releaseThreshold: LocalDateTime,
    @Param("currentTime") currentTime: LocalDateTime,
  ): Int

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

  fun findByRequestDateTimeBefore(thresholdTime: LocalDateTime): List<SubjectAccessRequest?>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "javax.persistence.lock.timeout", value = LOCK_TIMEOUT)])
  @Query(
    "SELECT s FROM SubjectAccessRequest s " +
      "WHERE :threshold > s.requestDateTime " +
      "AND s.status = 'Pending' " +
      "ORDER BY s.requestDateTime ASC",
  )
  fun findAllPendingSubjectAccessRequestsSubmittedBefore(@Param("threshold") threshold: LocalDateTime): List<SubjectAccessRequest?>

  @Lock(LockModeType.PESSIMISTIC_READ)
  @QueryHints(value = [QueryHint(name = "javax.persistence.lock.timeout", value = LOCK_TIMEOUT)])
  @Query("SELECT COUNT(1) FROM SubjectAccessRequest s WHERE s.status = :status")
  fun countSubjectAccessRequestsByStatus(@Param("status") status: Status): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE SubjectAccessRequest s SET s.status = 'Errored' WHERE s.id = :id AND s.status = 'Pending' AND :threshold > s.requestDateTime")
  fun updateStatusToErrorSubmittedBefore(@Param("id") id: UUID, @Param("threshold") threshold: LocalDateTime): Int
}
