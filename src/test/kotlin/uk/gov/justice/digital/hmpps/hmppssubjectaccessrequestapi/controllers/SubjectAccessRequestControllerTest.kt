package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.UUID

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
    Mockito.`when`(sarService.createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)).thenReturn("")
    val result = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)
    val expected: ResponseEntity<String> = ResponseEntity("", HttpStatus.OK)
    verify(sarService, times(1)).createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)
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
    Mockito.`when`(sarService.createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)).thenReturn("Both nomisId and ndeliusId are provided - exactly one is required")
    val response = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)
    verify(sarService, times(1)).createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)
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
    Mockito.`when`(sarService.createSubjectAccessRequest(noIDRequest, authentication, requestTime)).thenReturn("Neither nomisId nor ndeliusId is provided - exactly one is required")
    val response = SubjectAccessRequestController(sarService, auditService)
      .createSubjectAccessRequest(noIDRequest, authentication, requestTime)
    verify(sarService, times(1)).createSubjectAccessRequest(noIDRequest, authentication, requestTime)
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
    fun `claimSubjectAccessRequest returns 400 if updateSubjectAccessRequest returns 0 with time update`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      Mockito.`when`(sarService.claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))).thenReturn(0)
      val result = SubjectAccessRequestController(sarService, auditService)
        .claimSubjectAccessRequest(testUuid)
      verify(sarService, times(1)).claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))
      Assertions.assertThat(result).isEqualTo(400)
    }

    @Test
    fun `claimSubjectAccessRequest returns 200 if updateSubjectAccessRequest returns 1 with time update`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      Mockito.`when`(sarService.claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))).thenReturn(1)
      val result = SubjectAccessRequestController(sarService, auditService)
        .claimSubjectAccessRequest(testUuid)
      verify(sarService, times(1)).claimSubjectAccessRequest(eq(testUuid), any(LocalDateTime::class.java))
      Assertions.assertThat(result).isEqualTo(200)
    }

    @Test
    fun `completeSubjectAccessRequest returns 400 if completeSubjectAccessRequest returns 0 with status update`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      Mockito.`when`(sarService.completeSubjectAccessRequest(testUuid)).thenReturn(0)
      val result = SubjectAccessRequestController(sarService, auditService)
        .completeSubjectAccessRequest(testUuid)
      verify(sarService, times(1)).completeSubjectAccessRequest(testUuid)
      Assertions.assertThat(result).isEqualTo(400)
    }

    @Test
    fun `completeSubjectAccessRequest returns 200 if completeSubjectAccessRequest returns 1 with status update`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      Mockito.`when`(sarService.completeSubjectAccessRequest(testUuid)).thenReturn(1)
      val result = SubjectAccessRequestController(sarService, auditService)
        .completeSubjectAccessRequest(testUuid)
      verify(sarService, times(1)).completeSubjectAccessRequest(testUuid)
      Assertions.assertThat(result).isEqualTo(200)
    }
  }
}
