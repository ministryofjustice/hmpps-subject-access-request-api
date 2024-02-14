package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.DocumentStorageGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.gateways.SubjectAccessRequestGateway
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.models.SubjectAccessRequest
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
  private val documentGateway: DocumentStorageGateway = Mockito.mock(DocumentStorageGateway::class.java)
  private val uuid = UUID.randomUUID()

  @Nested
  inner class createSubjectAccessRequestPost {
    @Test
    fun `createSubjectAccessRequestPost and returns empty string`() {
      Mockito.`when`(authentication.name).thenReturn("aName")
      val expected = ""
      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(ndeliusRequest, authentication, requestTime)
      verify(sarGateway, times(1)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequestPost returns error string if both IDs are supplied`() {
      Mockito.`when`(authentication.name).thenReturn("aName")
      val expected =
        "Both nomisId and ndeliusId are provided - exactly one is required"
      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(ndeliusAndNomisRequest, authentication, requestTime)
      verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createSubjectAccessRequestPost returns error string if neither ID is supplied`() {
      Mockito.`when`(authentication.name).thenReturn("aName")
      val expected =
        "Neither nomisId nor ndeliusId is provided - exactly one is required"
      val result: String = SubjectAccessRequestService(sarGateway, documentGateway)
        .createSubjectAccessRequest(noIDRequest, authentication, requestTime)
      verify(sarGateway, times(0)).saveSubjectAccessRequest(sampleSAR)
      Assertions.assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class updateSubjectAccessRequest {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    @Test
    fun `claimSubjectAccessRequest calls gateway update method with time 5 minutes ago`() {
      val mockedCurrentTime = "02/01/2024 00:30"
      val formattedMockedCurrentTime = LocalDateTime.parse(mockedCurrentTime, dateTimeFormatter)
      val fiveMinutesAgo = "02/01/2024 00:25"
      val fiveMinutesAgoFormatted = LocalDateTime.parse(fiveMinutesAgo, dateTimeFormatter)
      SubjectAccessRequestService(sarGateway, documentGateway)
        .claimSubjectAccessRequest(uuid, formattedMockedCurrentTime)
      verify(sarGateway, times(1)).updateSubjectAccessRequestClaim(
        uuid,
        fiveMinutesAgoFormatted,
        formattedMockedCurrentTime,
      )
    }

    @Test
    fun `completeSubjectAccessRequest calls gateway update method with status`() {
      SubjectAccessRequestService(sarGateway, documentGateway)
        .completeSubjectAccessRequest(uuid)
      verify(sarGateway, times(1)).updateSubjectAccessRequestStatusCompleted(uuid)
    }
  }

  @Nested
  inner class documentRetrieval {
    private val expectedRetrievalResponse = JSONObject(
      "{\n" +
        "  \"documentUuid\": \"MockUUID\",\n" +
        "  \"documentType\": \"HMCTS_WARRANT\",\n" +
        "  \"documentFilename\": \"warrant_for_remand\",\n" +
        "  \"filename\": \"warrant_for_remand\",\n" +
        "  \"fileExtension\": \"pdf\",\n" +
        "  \"fileSize\": 48243,\n" +
        "  \"fileHash\": \"d58e3582afa99040e27b92b13c8f2280\",\n" +
        "  \"mimeType\": \"pdf\",\n" +
        "  \"metadata\": {\n" +
        "    \"prisonCode\": \"KMI\",\n" +
        "    \"prisonNumber\": \"C3456DE\",\n" +
        "    \"court\": \"Birmingham Magistrates\",\n" +
        "    \"warrantDate\": \"2023-11-14\"\n" +
        "  },\n" +
        "  \"createdTime\": \"2024-02-14T07:19:32.931Z\",\n" +
        "  \"createdByServiceName\": \"Remand and Sentencing\",\n" +
        "  \"createdByUsername\": \"AAA01U\"\n" +
        "}",
    )

    @Test
    fun `retrieveSubjectAccessRequestDocument calls document gateway retrieve method with id`() {
      SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument("MockUUID")
      verify(documentGateway, times(1)).retrieveDocument("MockUUID")
    }

    @Test
    fun `retrieveSubjectAccessRequestDocument returns JSONObject`() {
      Mockito.`when`(documentGateway.retrieveDocument("MockUUID")).thenReturn(expectedRetrievalResponse)
      val result = SubjectAccessRequestService(sarGateway, documentGateway).retrieveSubjectAccessRequestDocument("MockUUID")
      verify(documentGateway, times(1)).retrieveDocument("MockUUID")
      Assertions.assertThat(result).isEqualTo(expectedRetrievalResponse)
    }
  }
}
