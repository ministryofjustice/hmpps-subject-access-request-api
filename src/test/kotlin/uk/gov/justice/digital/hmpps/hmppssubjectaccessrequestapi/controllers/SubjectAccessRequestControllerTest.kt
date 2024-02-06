package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubjectAccessRequestControllerTest {
  private val requestTime = LocalDateTime.now()
  private val sarService = Mockito.mock(SubjectAccessRequestService::class.java)
  private val auditService = Mockito.mock(AuditService::class.java)
  private val authentication: Authentication = Mockito.mock(Authentication::class.java)

  @Test
  fun `createSubjectAccessRequestPost calls service createSubjectAccessRequestPost with same parameters`() {
    val ndeliusRequest = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '', " +
      "ndeliusId: '1' " +
      "}"
    Mockito.`when`(authentication.name).thenReturn("aName")
    Mockito.`when`(sarService.createSubjectAccessRequestPost(ndeliusRequest, authentication, requestTime)).thenReturn("")
    val result = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequestPost(ndeliusRequest, authentication, requestTime)
    val expected: ResponseEntity<String> = ResponseEntity("", HttpStatus.OK)
    verify(sarService, times(1)).createSubjectAccessRequestPost(ndeliusRequest, authentication, requestTime)
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns http error if both nomis and ndelius ids are provided`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val ndeliusAndNomisRequest = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '1', " +
      "ndeliusId: '1' " +
      "}"
    Mockito.`when`(sarService.createSubjectAccessRequestPost(ndeliusAndNomisRequest, authentication, requestTime)).thenReturn("Both nomisId and ndeliusId are provided - exactly one is required")
    val response = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequestPost(ndeliusAndNomisRequest, authentication, requestTime)
    verify(sarService, times(1)).createSubjectAccessRequestPost(ndeliusAndNomisRequest, authentication, requestTime)
    val expected: ResponseEntity<String> = ResponseEntity("Both nomisId and ndeliusId are provided - exactly one is required", HttpStatus.BAD_REQUEST)
    Assertions.assertThat(response).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns http error if neither nomis nor ndelius ids are provided`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val noIDRequest = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '', " +
      "ndeliusId: '' " +
      "}"
    Mockito.`when`(sarService.createSubjectAccessRequestPost(noIDRequest, authentication, requestTime)).thenReturn("Neither nomisId nor ndeliusId is provided - exactly one is required")
    val response = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequestPost(noIDRequest, authentication, requestTime)
    verify(sarService, times(1)).createSubjectAccessRequestPost(noIDRequest, authentication, requestTime)
    val expected: ResponseEntity<String> = ResponseEntity("Neither nomisId nor ndeliusId is provided - exactly one is required", HttpStatus.BAD_REQUEST)
    Assertions.assertThat(response).isEqualTo(expected)
  }

  @Test
  fun `getSubjectAccessRequests is called with unclaimedOnly = true if specified in controller and returns list`() {
    val result: List<SubjectAccessRequest?> = SubjectAccessRequestController(sarService, auditService)
      .getSubjectAccessRequests(unclaimed = true)
    verify(sarService, times(1)).getSubjectAccessRequests(unclaimedOnly = true)
    Assertions.assertThatList(result)
  }

  @Test
  fun `getSubjectAccessRequests is called with unclaimedOnly = false if unspecified in controller`() {
    SubjectAccessRequestController(sarService, auditService)
      .getSubjectAccessRequests()
    verify(sarService, times(1)).getSubjectAccessRequests(unclaimedOnly = false)
  }

  @Nested
  inner class patchSubjectAccessRequest {
    @Test
    fun `patch subjectAccessRequestClaim returns 400 if updateSubjectAccessRequest returns 0 with time update`() {
      val mockedCurrentTime = "02/01/2024 00:30"
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, formatter)
      Mockito.`when`(sarService.updateSubjectAccessRequestClaim(1, time = formattedMockedCurrentTime)).thenReturn(0)
      val result = SubjectAccessRequestController(sarService, auditService)
        .updateSubjectAccessRequest(1, time = formattedMockedCurrentTime)
      verify(sarService, times(1)).updateSubjectAccessRequestClaim(1, time = formattedMockedCurrentTime)
      Assertions.assertThat(result).isEqualTo(400)
    }

    @Test
    fun `patch subjectAccessRequestClaim returns 200 if updateSubjectAccessRequest returns 1 with time update`() {
      val mockedCurrentTime = "02/01/2024 00:30"
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, formatter)
      Mockito.`when`(sarService.updateSubjectAccessRequestClaim(1, time = formattedMockedCurrentTime)).thenReturn(1)
      val result = SubjectAccessRequestController(sarService, auditService)
        .updateSubjectAccessRequest(1, time = formattedMockedCurrentTime)
      verify(sarService, times(1)).updateSubjectAccessRequestClaim(1, time = formattedMockedCurrentTime)
      Assertions.assertThat(result).isEqualTo(200)
    }

    @Test
    fun `patch subjectAccessRequestStatus returns 400 if updateSubjectAccessRequest returns 0 with status update`() {
      Mockito.`when`(sarService.updateSubjectAccessRequestStatus(1)).thenReturn(0)
      val result = SubjectAccessRequestController(sarService, auditService)
        .updateSubjectAccessRequest(1, time = null)
      verify(sarService, times(1)).updateSubjectAccessRequestStatus(1)
      Assertions.assertThat(result).isEqualTo(400)
    }

    @Test
    fun `patch subjectAccessRequest returns 200 if updateSubjectAccessRequest returns 1 with status update`() {
      Mockito.`when`(sarService.updateSubjectAccessRequestStatus(1)).thenReturn(1)
      val result = SubjectAccessRequestController(sarService, auditService)
        .updateSubjectAccessRequest(1, time = null)
      verify(sarService, times(1)).updateSubjectAccessRequestStatus(1)
      Assertions.assertThat(result).isEqualTo(200)
    }
  }
}
