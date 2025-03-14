package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.AlertsConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.config.OverdueAlertConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ReportsOverdueSummary
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class SubjectAccessRequestControllerGetOverdueIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @MockitoBean
  private lateinit var alertConfiguration: AlertsConfiguration
  private var overdueAlertConfig: OverdueAlertConfiguration = mock()

  companion object {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateFrom = LocalDate.parse("30/12/2023", dateFormatter)
    private val dateTo = LocalDate.parse("30/01/2024", dateFormatter)
    private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateTimeNow = LocalDateTime.now().minusNanos(0)

    private val overduePendingRequest = subjectAccessRequestSubmittedAt(dateTimeNow.minusHours(5), Status.Pending)
    private val oldCompleteRequest = subjectAccessRequestSubmittedAt(dateTimeNow.minusHours(10), Status.Completed)
    private val newPendingRequest = subjectAccessRequestSubmittedAt(dateTimeNow.minusMinutes(5), Status.Pending)

    @JvmStatic
    fun nonOverdueReports(): List<TestCase?> = listOf(
      TestCase(null, "no reports exist"),
      TestCase(oldCompleteRequest, "status complete, processing duration greater than overdue threshold"),
      TestCase(newPendingRequest, "status pending, processing duration less than overdue threshold"),
    )

    private fun subjectAccessRequestSubmittedAt(
      requestSubmittedAt: LocalDateTime,
      status: Status,
    ): SubjectAccessRequest = SubjectAccessRequest(
      id = UUID.randomUUID(),
      status = status,
      dateFrom = dateFrom,
      dateTo = dateTo,
      sarCaseReferenceNumber = "666xzy",
      services = "{1,2,4}",
      nomisId = "",
      ndeliusCaseReferenceId = "hansGruber99",
      requestedBy = "Hans Gruber",
      requestDateTime = requestSubmittedAt,
      claimAttempts = 0,
      claimDateTime = null,
    )
  }

  @BeforeEach
  fun setup() {
    whenever(alertConfiguration.overdueAlertConfig).thenReturn(overdueAlertConfig)
    whenever(overdueAlertConfig.threshold).thenReturn(1)
    whenever(overdueAlertConfig.thresholdChronoUnit).thenReturn(ChronoUnit.HOURS)
    whenever(overdueAlertConfig.thresholdAsString()).thenReturn("1 Hours")
    whenever(overdueAlertConfig.calculateOverdueThreshold()).thenReturn(dateTimeNow.minusHours(1))

    subjectAccessRequestRepository.deleteAll()
  }

  @ParameterizedTest
  @MethodSource("nonOverdueReports")
  fun `should return empty list if no reports match the overdue criteria`(testCase: TestCase) {
    testCase.request?.let { subjectAccessRequestRepository.save(it) }

    val summary = getOverdueSubjectAccessRequests()

    assertThat(summary).isNotNull
    assertThat(summary!!.overdueThreshold).isEqualTo("1 Hours")
    assertThat(summary.requests).isEmpty()
    assertThat(summary.total).isEqualTo(0)
  }

  @Test
  fun `should return subject access request with processing time greater than overdue threshold and status pending`() {
    subjectAccessRequestRepository.saveAll(listOf(overduePendingRequest))

    val summary = getOverdueSubjectAccessRequests()

    assertThat(summary).isNotNull
    assertThat(summary!!.overdueThreshold).isEqualTo("1 Hours")
    assertThat(summary.requests).hasSize(1)
    assertThat(summary.total).isEqualTo(1)

    val overdueRequest = summary.requests[0]
    assertThat(overdueRequest).isNotNull
    assertThat(overdueRequest!!.id).isEqualTo(overduePendingRequest.id)
    assertThat(overdueRequest.sarCaseReferenceNumber).isEqualTo(overduePendingRequest.sarCaseReferenceNumber)
    assertThat(overdueRequest.submitted!!.formatted()).isEqualTo(overduePendingRequest.requestDateTime.formatted())
    assertThat(overdueRequest.claimsAttempted).isEqualTo(0)
    assertThat(overdueRequest.lastClaimed).isEqualTo(overduePendingRequest.claimDateTime)

    val expectedDuration = Duration.between(overduePendingRequest.requestDateTime, dateTimeNow)
    assertThat(overdueRequest.overdueDuration.toHoursPart()).isEqualTo(expectedDuration.toHoursPart())
    assertThat(overdueRequest.overdueDuration.toMinutesPart()).isEqualTo(expectedDuration.toMinutesPart())
  }

  fun getOverdueSubjectAccessRequests(): ReportsOverdueSummary? = webTestClient
    .get()
    .uri("/api/subjectAccessRequests/overdue")
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_SUPPORT")))
    .exchange()
    .expectStatus().isOk
    .returnResult(ReportsOverdueSummary::class.java)
    .responseBody
    .blockFirst()

  /**
   * Format the dateTime to "yyyy-MM-dd HH:mm"
   */
  fun LocalDateTime.formatted(): String? = dateTimeFormat.format(this)

  data class TestCase(
    val request: SubjectAccessRequest?,
    val description: String,
  ) {
    override fun toString(): String = description
  }
}
