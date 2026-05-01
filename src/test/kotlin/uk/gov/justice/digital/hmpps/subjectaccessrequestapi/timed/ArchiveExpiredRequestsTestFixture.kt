package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ArchivedSubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RenderStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.RequestServiceDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.Status
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.SubjectAccessRequest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class ArchiveExpiredRequestsTestFixture {

  protected val dateTimeNow: LocalDateTime = LocalDateTime.now()
  protected val dateNow: LocalDate = LocalDate.now()

  protected val serviceConfigOne = ServiceConfiguration(
    id = UUID.fromString("49b6b8b9-1cbd-4770-8b00-9431ebb17da0"),
    serviceName = "service-one",
    label = "Service One",
    url = "http://service-one.com",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PROBATION,
    suspended = false,
    suspendedAt = null,
  )

  protected val serviceConfigTwo = ServiceConfiguration(
    id = UUID.fromString("99cca786-bd32-443a-9666-b4d70a52c96a"),
    serviceName = "service-two",
    label = "Service Two",
    url = "http://service-two.com",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
    suspended = false,
    suspendedAt = null,
  )

  protected val serviceConfigThree = ServiceConfiguration(
    id = UUID.fromString("24ead987-e9f9-4ce1-8dd8-64f51ee1599f"),
    serviceName = "service-three",
    label = "Service Three",
    url = "http://service-three.com",
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
    suspended = false,
    suspendedAt = null,
  )

  protected val sar1 = SubjectAccessRequest(
    id = UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"),
    status = Status.Pending,
    dateFrom = dateNow.minusYears(3),
    dateTo = dateNow,
    sarCaseReferenceNumber = "AAA",
    nomisId = "AAA111",
    ndeliusCaseReferenceId = null,
    requestedBy = "Bob",
    requestDateTime = dateTimeNow,
    claimDateTime = dateTimeNow,
    claimAttempts = 5,
    objectUrl = null,
    lastDownloaded = null,
  ).apply {
    addServices(
      RequestServiceDetail(
        id = UUID.fromString("e114a3c8-8402-495f-8ea9-e9d430e6bb43"),
        subjectAccessRequest = this,
        serviceConfiguration = serviceConfigOne,
        renderStatus = RenderStatus.PENDING,
        templateVersion = TemplateVersion(
          id = UUID.fromString("d6800ae9-97be-42af-8bc1-cfeddb3f90bb"),
          serviceConfiguration = serviceConfigOne,
          status = TemplateVersionStatus.PUBLISHED,
          version = 1,
          createdAt = dateTimeNow.minusDays(2),
          publishedAt = dateTimeNow.minusDays(1),
          fileHash = "2eedacba-48fb-4237-b9ad-26d74f2e9d05",
        ),
        renderedAt = null,
      ),
      RequestServiceDetail(
        id = UUID.fromString("a841682a-1f34-4912-8221-5898c238cc47"),
        subjectAccessRequest = this,
        serviceConfiguration = serviceConfigTwo,
        renderStatus = RenderStatus.PENDING,
        templateVersion = TemplateVersion(
          id = UUID.fromString("671b0d13-89e0-493e-8124-2fc99647e679"),
          serviceConfiguration = serviceConfigTwo,
          status = TemplateVersionStatus.PUBLISHED,
          version = 2,
          createdAt = dateTimeNow.minusDays(2),
          publishedAt = dateTimeNow.minusDays(1),
          fileHash = "ccb9e341-58aa-4f52-b8a5-e9fe5512c8ac",
        ),
        renderedAt = null,
      ),
      RequestServiceDetail(
        id = UUID.fromString("a7c68158-536c-413e-ad86-8e873b51afe5"),
        subjectAccessRequest = this,
        serviceConfiguration = serviceConfigThree,
        renderStatus = RenderStatus.PENDING,
        templateVersion = TemplateVersion(
          id = UUID.fromString("bb779bad-d80e-477c-8065-ffe488f6c7c8"),
          serviceConfiguration = serviceConfigTwo,
          status = TemplateVersionStatus.PUBLISHED,
          version = 3,
          createdAt = dateTimeNow.minusDays(2),
          publishedAt = dateTimeNow.minusDays(1),
          fileHash = "2e56a44c-d70e-4c13-92bb-e46e7b293118",
        ),
        renderedAt = null,
      ),
    )
  }

  protected val sar2 = SubjectAccessRequest(
    id = UUID.fromString("1776d596-cc13-4060-a836-fe94be951615"),
    status = Status.Pending,
    dateFrom = dateNow.minusYears(3),
    dateTo = dateNow,
    sarCaseReferenceNumber = "BBB",
    nomisId = "BBB222",
    ndeliusCaseReferenceId = null,
    requestedBy = "Sally",
    requestDateTime = dateTimeNow,
    claimDateTime = dateTimeNow,
    claimAttempts = 5,
    objectUrl = null,
    lastDownloaded = null,
  ).apply {
    addServices(
      RequestServiceDetail(
        id = UUID.fromString("4b7a4959-8618-44cb-b539-fefd4be6f299"),
        subjectAccessRequest = this,
        serviceConfiguration = serviceConfigOne,
        renderStatus = RenderStatus.PENDING,
        templateVersion = TemplateVersion(
          id = UUID.fromString("6d7b88b4-70be-481f-8d05-c7300899f211"),
          serviceConfiguration = serviceConfigOne,
          status = TemplateVersionStatus.PUBLISHED,
          version = 1,
          createdAt = dateTimeNow.minusDays(2),
          publishedAt = dateTimeNow.minusDays(1),
          fileHash = "26c2ae538-93be-4191-8ed0-6f1bc638a6fa",
        ),
        renderedAt = null,
      ),
    )
  }

  protected val sar3 = SubjectAccessRequest(
    id = UUID.fromString("88f20a4d-1642-4ff8-8736-3cabc21309f2"),
    status = Status.Pending,
    dateFrom = dateNow.minusYears(3),
    dateTo = dateNow,
    sarCaseReferenceNumber = "CCC",
    nomisId = "CCC333",
    ndeliusCaseReferenceId = null,
    requestedBy = "Bert",
    requestDateTime = dateTimeNow,
    claimDateTime = dateTimeNow,
    claimAttempts = 3,
    objectUrl = null,
    lastDownloaded = null,
  ).apply {
    addServices(
      RequestServiceDetail(
        id = UUID.fromString("d8768b5c-9b5d-4c0c-bf2c-4e0b8bc64852"),
        subjectAccessRequest = this,
        serviceConfiguration = serviceConfigThree,
        renderStatus = RenderStatus.PENDING,
        templateVersion = TemplateVersion(
          id = UUID.fromString("3b51e027-12c4-4aff-abbb-a732a51bf4e9"),
          serviceConfiguration = serviceConfigOne,
          status = TemplateVersionStatus.PUBLISHED,
          version = 1,
          createdAt = dateTimeNow.minusDays(2),
          publishedAt = dateTimeNow.minusDays(1),
          fileHash = "84f82630-4e8f-45fc-8864-5c41266f01bd",
        ),
        renderedAt = null,
      ),
    )
  }

  @Suppress("ktlint:standard:property-naming")
  protected val archivedSAR_1_1 = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "AAA",
    sarNomisId = "AAA111",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Bob",
    sarRequestDateTime = dateTimeNow,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 5,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.fromString("49b6b8b9-1cbd-4770-8b00-9431ebb17da0"),
    serviceName = "service-one",
    serviceLabel = "Service One",
    serviceUrl = "http://service-one.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PROBATION,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.fromString("d6800ae9-97be-42af-8bc1-cfeddb3f90bb"),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 1,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = "2eedacba-48fb-4237-b9ad-26d74f2e9d05",
  )

  @Suppress("ktlint:standard:property-naming")
  protected val archivedSAR_1_2 = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "AAA",
    sarNomisId = "AAA111",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Bob",
    sarRequestDateTime = dateTimeNow,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 5,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.fromString("99cca786-bd32-443a-9666-b4d70a52c96a"),
    serviceName = "service-two",
    serviceLabel = "Service Two",
    serviceUrl = "http://service-two.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PRISON,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.fromString("671b0d13-89e0-493e-8124-2fc99647e679"),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 2,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = "ccb9e341-58aa-4f52-b8a5-e9fe5512c8ac",
  )

  @Suppress("ktlint:standard:property-naming")
  protected val archivedSAR_1_3 = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.fromString("f8615d62-3d8c-4eae-bb85-57f2ae29b88e"),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "AAA",
    sarNomisId = "AAA111",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Bob",
    sarRequestDateTime = dateTimeNow,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 5,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.fromString("24ead987-e9f9-4ce1-8dd8-64f51ee1599f"),
    serviceName = "service-three",
    serviceLabel = "Service Three",
    serviceUrl = "http://service-three.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PRISON,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.fromString("bb779bad-d80e-477c-8065-ffe488f6c7c8"),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 3,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = "2e56a44c-d70e-4c13-92bb-e46e7b293118",
  )

  @Suppress("ktlint:standard:property-naming")
  protected val archivedSAR_2_1 = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.fromString("1776d596-cc13-4060-a836-fe94be951615"),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "BBB",
    sarNomisId = "BBB222",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Sally",
    sarRequestDateTime = dateTimeNow,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 5,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.fromString("49b6b8b9-1cbd-4770-8b00-9431ebb17da0"),
    serviceName = "service-one",
    serviceLabel = "Service One",
    serviceUrl = "http://service-one.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PROBATION,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.fromString("6d7b88b4-70be-481f-8d05-c7300899f211"),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 1,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = "26c2ae538-93be-4191-8ed0-6f1bc638a6fa",
  )

  @Suppress("ktlint:standard:property-naming")
  protected val archivedSAR_3_1 = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.fromString("88f20a4d-1642-4ff8-8736-3cabc21309f2"),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "CCC",
    sarNomisId = "CCC333",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Bert",
    sarRequestDateTime = dateTimeNow,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 3,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.fromString("24ead987-e9f9-4ce1-8dd8-64f51ee1599f"),
    serviceName = "service-three",
    serviceLabel = "Service Three",
    serviceUrl = "http://service-three.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PRISON,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.fromString("3b51e027-12c4-4aff-abbb-a732a51bf4e9"),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 1,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = "84f82630-4e8f-45fc-8864-5c41266f01bd",
  )

  @Suppress("ktlint:standard:property-naming")
  fun archiveRequestWithRequestDateTime(requestDateTime: LocalDateTime) = ArchivedSubjectAccessRequest(
    id = UUID.randomUUID(),
    sarId = UUID.randomUUID(),
    sarStatus = Status.Pending,
    sarDateFrom = dateNow.minusYears(3),
    sarDateTo = dateNow,
    sarCaseReferenceNumber = "CCC",
    sarNomisId = "CCC333",
    sarNdeliusCaseReferenceId = null,
    sarRequestedBy = "Bert",
    sarRequestDateTime = requestDateTime,
    sarClaimDateTime = dateTimeNow,
    sarClaimAttempts = 3,
    sarObjectUrl = null,
    sarLastDownloaded = null,
    serviceId = UUID.randomUUID(),
    serviceName = "service-three",
    serviceLabel = "Service Three",
    serviceUrl = "http://service-three.com",
    serviceEnabled = true,
    serviceTemplateMigrated = true,
    serviceCategory = ServiceCategory.PRISON,
    serviceSuspended = false,
    serviceSuspendedAt = null,
    requestRenderStatus = RenderStatus.PENDING,
    requestRenderedAt = null,
    templateVersionId = UUID.randomUUID(),
    templateVersionStatus = TemplateVersionStatus.PUBLISHED,
    templateVersion = 1,
    templateVersionCreatedAt = dateTimeNow.minusDays(2),
    templateVersionPublishedAt = dateTimeNow.minusDays(1),
    templateVersionFileHash = UUID.randomUUID().toString(),
  )
}
