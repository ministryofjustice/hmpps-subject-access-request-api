package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.controllers

import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services.AuditService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class SubjectAccessRequestControllerTest {
  @Test
  fun `createSubjectAccessRequestPost returns 200 and passes data to repository`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")

    val request = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '', " +
      "ndeliusCaseReferenceId: '1' " +
      "}"
    val requestTime = LocalDateTime.now()

    val expected = ResponseEntity("",  HttpStatus.OK)
    val result: ResponseEntity<String> = SubjectAccessRequestController(auditService, sarRepository)
      .createSubjectAccessRequestPost(request, authentication, requestTime)

    val json = JSONObject(request)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateFrom = json.get("dateFrom").toString()
    val dateFromFormatted = LocalDate.parse(dateFrom, formatter)

    val dateTo = json.get("dateTo").toString()
    val dateToFormatted = LocalDate.parse(dateTo, formatter)

    verify(sarRepository, times(1)).save(
      SubjectAccessRequest(
        id = null,
        status = Status.Pending,
        dateFrom = dateFromFormatted,
        dateTo = dateToFormatted,
        sarCaseReferenceNumber = "1234abc",
        services = "{1,2,4}",
        nomisId = "",
        ndeliusCaseReferenceId = "1",
        requestedBy = authentication.name,
        requestDateTime = requestTime,
      ),
    )
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if both IDs are supplied`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")

    val request = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '1', " +
      "ndeliusCaseReferenceId: '1' " +
      "}"

    val expected = ResponseEntity("Both nomisId and ndeliusCaseReferenceId are provided - exactly one is required",  HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestController(auditService, sarRepository)
      .createSubjectAccessRequestPost(request, authentication)

    verify(sarRepository, times(0)).save(any())
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if neither ID is supplied`() {
    val sarRepository = Mockito.mock(SubjectAccessRequestRepository::class.java)
    val auditService = Mockito.mock(AuditService::class.java)
    val authentication: Authentication = Mockito.mock(Authentication::class.java)
    Mockito.`when`(authentication.name).thenReturn("aName")

    val request = "{ " +
      "dateFrom: '01/12/2023', " +
      "dateTo: '03/01/2024', " +
      "sarCaseReferenceNumber: '1234abc', " +
      "services: '{1,2,4}', " +
      "nomisId: '', " +
      "ndeliusCaseReferenceId: '' " +
      "}"

    val expected = ResponseEntity("Neither nomisId nor ndeliusCaseReferenceId is provided - exactly one is required",  HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestController(auditService, sarRepository)
      .createSubjectAccessRequestPost(request, authentication)

    verify(sarRepository, times(0)).save(any())
    Assertions.assertThat(result).isEqualTo(expected)
  }
}