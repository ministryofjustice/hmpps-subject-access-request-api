package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class SubjectAccessRequestControllerTest {

  private val ndeliusRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: '', " +
    "ndeliusId: '1' " +
    "}"

  private val ndeliusAndNomisRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: '1', " +
    "ndeliusId: '1' " +
    "}"

  private val noIDRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: '', " +
    "ndeliusId: '' " +
    "}"

  private val json = JSONObject(ndeliusRequest)
  private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = json.get("dateFrom").toString()
  private val dateFromFormatted = LocalDate.parse(dateFrom, formatter)
  private val dateTo = json.get("dateTo").toString()
  private val dateToFormatted = LocalDate.parse(dateTo, formatter)
  private val requestTime = LocalDateTime.now()
  private val sampleSAR = SubjectAccessRequest(
    id = null,
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = "",
    ndeliusCaseReferenceId = "1",
    requestedBy = "aName",
    requestDateTime = requestTime,
    claimAttempts = 0,
  )

  private val sampleUnclaimedSAR = SubjectAccessRequest(
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
  @Test
  fun `createSubjectAccessRequestPost returns 200 and passes data to repository`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val subjectAccessRequestService = Mockito.mock(SubjectAccessRequestService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected = ResponseEntity("", HttpStatus.OK)
    val result: ResponseEntity<String> = SubjectAccessRequestController(subjectAccessRequestService, auditService, sarRepository)
      .createSubjectAccessRequestPost(ndeliusRequest, authentication, requestTime)
    verify(sarRepository, times(1)).save(sampleSAR)
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if both IDs are supplied`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    val subjectAccessRequestService = Mockito.mock(SubjectAccessRequestService::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected = ResponseEntity("Both nomisId and ndeliusId are provided - exactly one is required", HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestController(subjectAccessRequestService, auditService, sarRepository)
      .createSubjectAccessRequestPost(ndeliusAndNomisRequest, authentication, requestTime)
    verify(sarRepository, times(0)).save(any())
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if neither ID is supplied`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    val subjectAccessRequestService = Mockito.mock(SubjectAccessRequestService::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected = ResponseEntity("Neither nomisId nor ndeliusId is provided - exactly one is required", HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestController(subjectAccessRequestService, auditService, sarRepository)
      .createSubjectAccessRequestPost(noIDRequest, authentication, requestTime)
    verify(sarRepository, times(0)).save(any())
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `getSubjectAccessRequests is called with unclaimedOnly = true if specified in controller and returns list`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val subjectAccessRequestService = Mockito.mock(SubjectAccessRequestService::class.java)
    val result: List<SubjectAccessRequest?> = SubjectAccessRequestController(subjectAccessRequestService, auditService, sarRepository)
      .getSubjectAccessRequests(unclaimed = true)
    verify(subjectAccessRequestService, times(1)).getSubjectAccessRequests(unclaimedOnly = true)
    Assertions.assertThatList(result)
  }

  @Test
  fun `getSubjectAccessRequests is called with unclaimedOnly = false if unspecified in controller`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val subjectAccessRequestService = Mockito.mock(SubjectAccessRequestService::class.java)
    val result: List<SubjectAccessRequest?> = SubjectAccessRequestController(subjectAccessRequestService, auditService, sarRepository)
      .getSubjectAccessRequests()
    verify(subjectAccessRequestService, times(1)).getSubjectAccessRequests(unclaimedOnly = false)
  }

}
