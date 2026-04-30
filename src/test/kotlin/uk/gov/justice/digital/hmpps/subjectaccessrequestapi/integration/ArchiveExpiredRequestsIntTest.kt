package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.DocumentServiceApiExtension.Companion.documentServiceApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestArchiveRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.ArchiveExpiredRequestsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
class ArchiveExpiredRequestsIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  @Autowired
  private lateinit var subjectAccessRequestArchiveRepository: SubjectAccessRequestArchiveRepository

  @Autowired
  private lateinit var archiveExpiredRequestsService: ArchiveExpiredRequestsService

  @Autowired
  private lateinit var templateVersionRepository: TemplateVersionRepository

  lateinit var request: SubjectAccessRequest

  companion object {
    private const val EXPECTED_SERVICE_COUNT = 33

    private val dateTimeNow = LocalDateTime.now()
    private val dateNow = LocalDate.now()

    private val sarStatus = Status.Pending
    private val dateFrom = dateNow.minusYears(10)
    private val dateTo = dateNow
    private val sarCaseReferenceNumber = "1234567890"
    private val nomisId = "666"
    private val ndeliusCaseReferenceId = null
    private val requestedBy = "Bob"
    private val requestDateTime = dateTimeNow.minusYears(1)
    private val claimDateTime = dateTimeNow.minusMinutes(60)
    private val claimAttempts = 100
    private val templateVersionVersion = 1
    private val templateVersionCreatedAt = dateTimeNow.minusHours(2)
    private val templateVersionFileHash = UUID.randomUUID().toString()
    private val templateVersionStatus = TemplateVersionStatus.PUBLISHED
    private val templateVersionPublishedAt = dateTimeNow.minusHours(1)

    @JvmStatic
    fun errorStatuses() = listOf(400, 401, 403, 409, 500, 501, 502, 503, 504)
  }

  @BeforeEach
  fun setup() {
    hmppsAuth.stubGrantToken()
    createTemplateVersions()
    request = createSubjectAccessRequest()
  }

  @AfterEach
  fun cleanup() {
    request.let { subjectAccessRequestRepository.deleteById(it.id) }
    subjectAccessRequestArchiveRepository.deleteAll()
    templateVersionRepository.deleteAll()
  }

  @Test
  fun `should archive legacy requests`() {
    documentServiceApi.deleteDocumentSuccess(request.id)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    assertThat(subjectAccessRequestRepository.findByIdOrNull(request.id)).isNull()
    assertThat(subjectAccessRequestArchiveRepository.findAll()).hasSize(EXPECTED_SERVICE_COUNT)
    assertExpectedArchivedRequestsExists(request.id)

    documentServiceApi.deleteDocumentIsCalled(1, request.id)
  }

  @Test
  fun `should archive requests when SAR ID does not exist in DocumentStore`() {
    documentServiceApi.deleteDocumentError(request.id, 404)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    assertThat(subjectAccessRequestRepository.findByIdOrNull(request.id)).isNull()
    assertThat(subjectAccessRequestArchiveRepository.findAll()).hasSize(EXPECTED_SERVICE_COUNT)
    assertExpectedArchivedRequestsExists(request.id)

    documentServiceApi.deleteDocumentIsCalled(1, request.id)
  }

  @ParameterizedTest
  @MethodSource("errorStatuses")
  fun `should not archive request when DocumentStore returns error`(documentStoreStatus: Int) {
    documentServiceApi.deleteDocumentError(request.id, documentStoreStatus)

    archiveExpiredRequestsService.removeExpiredDocumentsAndArchiveRequests()

    assertThat(subjectAccessRequestRepository.findByIdOrNull(request.id)).isNotNull
    assertThat(subjectAccessRequestArchiveRepository.findAll()).hasSize(0)

    documentServiceApi.deleteDocumentIsCalled(1, request.id)
  }

  private fun assertExpectedArchivedRequestsExists(id: UUID) {
    serviceConfigurationRepository.findAll().forEach {
      val actual = getArchivedRequestByServiceNameAndSarId(id, it.serviceName)

      assertThat(actual.sarId).isEqualTo(request.id)
      assertThat(actual.sarStatus).isEqualTo(sarStatus)
      assertThat(actual.sarDateFrom).isEqualTo(dateFrom)
      assertThat(actual.sarDateTo).isEqualTo(dateTo)
      assertThat(actual.sarCaseReferenceNumber).isEqualTo(sarCaseReferenceNumber)
      assertThat(actual.sarNomisId).isEqualTo(nomisId)
      assertThat(actual.sarNdeliusCaseReferenceId).isEqualTo(ndeliusCaseReferenceId)
      assertThat(actual.sarRequestedBy).isEqualTo(requestedBy)
      assertThat(actual.sarRequestDateTime).isEqualTo(requestDateTime)
      assertThat(actual.sarClaimDateTime).isEqualTo(claimDateTime)
      assertThat(actual.sarClaimAttempts).isEqualTo(claimAttempts)
      assertThat(actual.sarObjectUrl).isNull()
      assertThat(actual.sarLastDownloaded).isNull()

      assertThat(actual.serviceId).isEqualTo(it.id)
      assertThat(actual.serviceName).isEqualTo(it.serviceName)
      assertThat(actual.serviceLabel).isEqualTo(it.label)
      assertThat(actual.serviceUrl).isEqualTo(it.url)
      assertThat(actual.serviceEnabled).isEqualTo(it.enabled)
      assertThat(actual.serviceTemplateMigrated).isEqualTo(it.templateMigrated)
      assertThat(actual.serviceCategory).isEqualTo(it.category)
      assertThat(actual.serviceSuspended).isEqualTo(it.suspended)
      assertThat(actual.serviceSuspendedAt).isEqualTo(it.suspendedAt)

      assertThat(actual.requestRenderStatus).isEqualTo(RenderStatus.PENDING)
      assertThat(actual.requestRenderedAt).isNull()

      val templateVersion = templateVersionRepository.findLatestByServiceConfigurationId(it.id)
      assertThat(templateVersion).isNotNull
      assertThat(actual.templateVersionId).isEqualTo(templateVersion!!.id)
      assertThat(actual.templateVersionStatus).isEqualTo(templateVersionStatus)
      assertThat(actual.templateVersion).isEqualTo(templateVersionVersion)
      assertThat(actual.templateVersionCreatedAt).isEqualTo(templateVersionCreatedAt)
      assertThat(actual.templateVersionPublishedAt).isEqualTo(templateVersionPublishedAt)
      assertThat(actual.templateVersionFileHash).isEqualTo(templateVersionFileHash)
    }
  }

  private fun getArchivedRequestByServiceNameAndSarId(
    sarId: UUID,
    serviceName: String,
  ): ArchivedSubjectAccessRequest {
    val actual = subjectAccessRequestArchiveRepository.findBySarIdAndServiceName(
      sarId = sarId,
      serviceName = serviceName,
    )

    assertThat(actual)
      .withFailMessage { "expected ArchivedSubjectAccessRequest id: $sarId, serviceName: $serviceName but not found" }
      .isNotNull

    return actual!!
  }

  private fun createTemplateVersions() {
    serviceConfigurationRepository.findAll().forEach {
      templateVersionRepository.saveAndFlush(
        TemplateVersion(
          id = UUID.randomUUID(),
          serviceConfiguration = it,
          version = 1,
          createdAt = templateVersionCreatedAt,
          fileHash = templateVersionFileHash,
          status = templateVersionStatus,
          publishedAt = templateVersionPublishedAt,
        ),
      )
    }
  }

  private fun createSubjectAccessRequest(): SubjectAccessRequest {
    val services = serviceConfigurationRepository.findAll()
    assertThat(services).isNotNull
    assertThat(services).hasSize(EXPECTED_SERVICE_COUNT)

    val request = SubjectAccessRequest(
      id = UUID.randomUUID(),
      status = sarStatus,
      dateFrom = dateFrom,
      dateTo = dateTo,
      sarCaseReferenceNumber = sarCaseReferenceNumber,
      services = mutableListOf(),
      nomisId = nomisId,
      ndeliusCaseReferenceId = ndeliusCaseReferenceId,
      requestedBy = requestedBy,
      requestDateTime = requestDateTime,
      claimDateTime = claimDateTime,
      claimAttempts = claimAttempts,
      objectUrl = null,
      lastDownloaded = null,
    )
    request.services.addAll(
      services.map {
        RequestServiceDetail(
          id = UUID.randomUUID(),
          serviceConfiguration = it,
          subjectAccessRequest = request,
          renderStatus = RenderStatus.PENDING,
          renderedAt = null,
          templateVersion = templateVersionRepository.findLatestByServiceConfigurationId(it.id),
        )
      },
    )
    return subjectAccessRequestRepository.saveAndFlush(request)
  }
}
