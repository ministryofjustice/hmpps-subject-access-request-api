package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SubjectAccessRequestServiceTest {

  private val ndeliusRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
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
    "nomisId: null, " +
    "ndeliusId: null " +
    "}"

  private val noDateToRequest = "{ " +
    "dateFrom: '01/12/2023', " +
    "dateTo: '', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  private val noDateFromRequest = "{ " +
    "dateFrom: '', " +
    "dateTo: '03/01/2024', " +
    "sarCaseReferenceNumber: '1234abc', " +
    "services: '{1,2,4}', " +
    "nomisId: null, " +
    "ndeliusId: '1' " +
    "}"

  private val json = JSONObject(ndeliusRequest)
  private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val dateFrom = json.get("dateFrom").toString()
  private val dateFromFormatted = LocalDate.parse(dateFrom, formatter)
  private val dateTo = json.get("dateTo").toString()
  private val dateToFormatted = LocalDate.parse(dateTo, formatter)
  private val requestTime = LocalDateTime.now()
  private val sampleSAR = SubjectAccessRequest(
    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    status = Status.Pending,
    dateFrom = dateFromFormatted,
    dateTo = dateToFormatted,
    sarCaseReferenceNumber = "1234abc",
    services = "{1,2,4}",
    nomisId = null,
    ndeliusCaseReferenceId = "1",
    requestedBy = "mockUserName",
    requestDateTime = requestTime,
    claimAttempts = 0,
  )
  private val sarGateway = Mockito.mock(SubjectAccessRequestGateway::class.java)
  private val authentication: Authentication = Mockito.mock(Authentication::class.java)
  private val documentGateway: DocumentStorageGateway = Mockito.mock(DocumentStorageGateway::class.java)
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  private val mockedCurrentTime = "02/01/2024 00:30"
  private val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)

  @Nested
  inner class CreateSubjectAccessRequest {
    @Test
    fun `createSubjectAccessRequest returns empty string`() {
      Mockito.`when`(authentication.name).thenReturn("mockUserName")
      val expected = ""

      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(ndeliusRequest, authentication, requestTime, sampleSAR.id)

      verify(sarGateway, times(1)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequest returns error string if both IDs are supplied`() {
      val expected =
        "Both nomisId and ndeliusId are provided - exactly one is required"

      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime, sampleSAR.id)

      verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequest returns error string if neither subject ID is supplied`() {
      val expected =
        "Neither nomisId nor ndeliusId is provided - exactly one is required"

      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(noIDRequest, authentication, requestTime, sampleSAR.id)

      verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateTo is not provided`() {
      Mockito.`when`(authentication.name).thenReturn("mockUserName")
      val expected = ""

      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(noDateToRequest, authentication, requestTime, sampleSAR.id)

      verify(sarGateway, times(1)).saveSubjectAccessRequest(any())
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequest does not error when dateFrom is not provided`() {
      Mockito.`when`(authentication.name).thenReturn("mockUserName")
      val expected = ""

      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(noDateFromRequest, authentication, requestTime, sampleSAR.id)

      verify(sarGateway, times(1)).saveSubjectAccessRequest(any())
      Assertions.assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class UpdateSubjectAccessRequest {

    @Test
    fun `claimSubjectAccessRequest calls gateway update method with time 5 minutes ago`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val fiveMinutesAgo = "02/01/2024 00:25"
      val fiveMinutesAgoFormatted = LocalDateTime.parse(fiveMinutesAgo, dateTimeFormatter)
      SubjectAccessRequestService(sarGateway, documentGateway)
        .claimSubjectAccessRequest(testUuid, formattedMockedCurrentTime)
      verify(sarGateway, times(1)).updateSubjectAccessRequestClaim(
        testUuid,
        fiveMinutesAgoFormatted,
        formattedMockedCurrentTime,
      )
    }

    @Test
    fun `completeSubjectAccessRequest calls gateway update method with status`() {
      val testUuid = UUID.fromString("55555555-5555-5555-5555-555555555555")
      SubjectAccessRequestService(sarGateway, documentGateway)
        .completeSubjectAccessRequest(testUuid)
      verify(sarGateway, times(1)).updateSubjectAccessRequestStatusCompleted(testUuid)
    }
  }

  @Nested
  inner class DocumentRetrieval {
    private val expectedRetrievalResponse = Mockito.mock(ByteArrayInputStream::class.java)
    val mockUUID = UUID.randomUUID()

    @Test
    fun `retrieveSubjectAccessRequestDocument calls document gateway retrieve method with id`() {
      SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument(mockUUID)
      verify(documentGateway, times(1)).retrieveDocument(mockUUID)
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument returns ResponseEntity if response from gateway is provided`() {
      val mockByteArrayInputStream = Mockito.mock(ByteArrayInputStream::class.java)
      val mockStream = Flux.just(InputStreamResource(mockByteArrayInputStream))
      Mockito.`when`(documentGateway.retrieveDocument(mockUUID)).thenReturn(ResponseEntity.ok().body(mockStream))
      val result = SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument(mockUUID)
      verify(documentGateway, times(1)).retrieveDocument(mockUUID)
      Assertions.assertThat(result).isEqualTo(ResponseEntity.ok().body(mockStream))
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument returns null if response from gateway is not provided`() {
      Mockito.`when`(documentGateway.retrieveDocument(mockUUID)).thenReturn(null)
      val result = SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument(mockUUID)
      verify(documentGateway, times(1)).retrieveDocument(mockUUID)
      Assertions.assertThat(result).isEqualTo(null)
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument calls SAR gateway updateLastDownloadedDateTime method with id and dateTime`() {
      SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument(mockUUID, formattedMockedCurrentTime)

      verify(sarGateway, times(1)).updateLastDownloadedDateTime(mockUUID, formattedMockedCurrentTime)
    }
  }

  @Nested
  inner class GetSubjectAccessRequests {
    @Test
    fun `getSubjectAccessRequests calls SAR gateway getSubjectAccessRequests method with specified arguments`() {
      SubjectAccessRequestService(sarGateway, documentGateway).getSubjectAccessRequests(unclaimedOnly = true, search = "testSearchString", pageNumber = 1, pageSize = 1)

      verify(sarGateway, times(1)).getSubjectAccessRequests(eq(true), eq("testSearchString"), eq(1), eq(1), any())
    }
  }

  @Nested
  inner class DeleteOldSubjectAccessRequests {
    @Test
    fun `deleteOldSubjectAccessRequests calls SAR gateway getOldSubjectAccessRequests`() {
      SubjectAccessRequestService(sarGateway, documentGateway).deleteOldSubjectAccessRequests()

      verify(sarGateway, times(1)).getOldSubjectAccessRequests(any())
    }

    @Test
    fun `deleteOldSubjectAccessRequests calls document gateway deleteDocument`() {
      Mockito.`when`(sarGateway.getOldSubjectAccessRequests(any())).thenReturn(listOf(sampleSAR))
      Mockito.`when`(documentGateway.deleteDocument(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(HttpStatus.NO_CONTENT)
      SubjectAccessRequestService(sarGateway, documentGateway).deleteOldSubjectAccessRequests()

      verify(documentGateway, times(1)).deleteDocument(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    }

    @Test
    fun `deleteOldSubjectAccessRequests calls SAR DB gateway deleteSubjectAccessRequest`() {
      Mockito.`when`(sarGateway.getOldSubjectAccessRequests(any())).thenReturn(listOf(sampleSAR))
      Mockito.`when`(documentGateway.deleteDocument(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(
        HttpStatus.NOT_FOUND,
      )
      SubjectAccessRequestService(sarGateway, documentGateway).deleteOldSubjectAccessRequests()

      verify(sarGateway, times(1)).deleteSubjectAccessRequest(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    }
  }
}
