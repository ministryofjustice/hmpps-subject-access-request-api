package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface SubjectAccessRequestRepository : JpaRepository<SubjectAccessRequest, Int> {
  fun findByClaimAttemptsIs(claimAttempts: Int): List<SubjectAccessRequest?>

  fun findByStatusIsAndClaimAttemptsGreaterThanAndClaimDateTimeBefore(status: Status, claimAttempts: Int, claimDateTime: LocalDateTime): List<SubjectAccessRequest?>

  @Modifying
  @Query('update User u set u.active = false where u.lastLoginDate < :date')
  fun deactivateUsersNotLoggedInSince(@Param("date") date: LocalDate?)
}
