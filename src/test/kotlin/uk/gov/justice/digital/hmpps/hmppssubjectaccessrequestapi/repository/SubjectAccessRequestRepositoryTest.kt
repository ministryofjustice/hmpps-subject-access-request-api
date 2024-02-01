package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DataJpaTest
class SubjectAccessRequestRepositoryTest {

  @Autowired
  private val sarRepository: SubjectAccessRequestRepository? = null
  private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = "01/12/2023"
  private val dateFromFormatted = LocalDate.parse(dateFrom, formatter)
  private val dateTo = "03/01/2024"
  private val dateToFormatted = LocalDate.parse(dateTo, formatter)
  private val requestTime = LocalDateTime.now()
  val sampleUnclaimedSAR = SubjectAccessRequest(
    id = null,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 0,
  )
  val sampleClaimedSAR = SubjectAccessRequest(
    id = null,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "Test",
    requestDateTime = requestTime,
    claimAttempts = 1,
  )

  @Test
  fun `findByClaimAttemptsIs returns only unclaimed SAR entries if called with 0`() {
    val expectedAll: List<SubjectAccessRequest> = listOf(sampleClaimedSAR, sampleUnclaimedSAR)
    val expectedUnclaimed: List<SubjectAccessRequest> = listOf(sampleUnclaimedSAR)
    sarRepository?.save(sampleClaimedSAR)
    sarRepository?.save(sampleUnclaimedSAR)
    Assertions.assertThat(sarRepository?.findAll()).isEqualTo(expectedAll)
    Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(0)).isEqualTo(expectedUnclaimed)
  }

  @Test
  fun `findByClaimAttemptsIs returns only claimed SAR entries if called with 1 or more`() {
    val expectedAll: List<SubjectAccessRequest> = listOf(sampleClaimedSAR, sampleUnclaimedSAR)
    val expectedClaimed: List<SubjectAccessRequest> = listOf(sampleClaimedSAR)
    sarRepository?.save(sampleClaimedSAR)
    sarRepository?.save(sampleUnclaimedSAR)
    Assertions.assertThat(sarRepository?.findAll()).isEqualTo(expectedAll)
    Assertions.assertThat(sarRepository?.findByClaimAttemptsIs(1)).isEqualTo(expectedClaimed)
  }

  @Test
  fun `db doesn't save between tests`() {
    val emptyList: List<Any> = emptyList()
    Assertions.assertThat(sarRepository?.findAll()).isEqualTo(emptyList)
  }
}
