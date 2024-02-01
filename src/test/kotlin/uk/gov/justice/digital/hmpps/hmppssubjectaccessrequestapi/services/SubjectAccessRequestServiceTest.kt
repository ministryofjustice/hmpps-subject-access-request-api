package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubjectAccessRequestServiceTest{

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
  private val sarGateway = Mockito.mock(SubjectAccessRequestGateway::class.java)
  private val authentication: Authentication = Mockito.mock(Authentication::class.java)
  @Test
  fun `createSubjectAccessRequestPost and returns 200`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected = ResponseEntity("", HttpStatus.OK)
    val result: ResponseEntity<String> = SubjectAccessRequestService(sarGateway)
      .createSubjectAccessRequestPost(ndeliusRequest, authentication, requestTime)
    verify(sarGateway, times(1)).saveSubjectAccessRequest(sampleSAR)
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if both IDs are supplied`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected =
      ResponseEntity("Both nomisId and ndeliusId are provided - exactly one is required", HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestService(sarGateway)
      .createSubjectAccessRequestPost(ndeliusAndNomisRequest, authentication, requestTime)
    verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
    Assertions.assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `createSubjectAccessRequestPost returns 400 and error string if neither ID is supplied`() {
    Mockito.`when`(authentication.name).thenReturn("aName")
    val expected =
      ResponseEntity("Neither nomisId nor ndeliusId is provided - exactly one is required", HttpStatus.BAD_REQUEST)
    val result: ResponseEntity<String> = SubjectAccessRequestService(sarGateway)
      .createSubjectAccessRequestPost(noIDRequest, authentication, requestTime)
    verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
    Assertions.assertThat(result).isEqualTo(expected)
  }
}