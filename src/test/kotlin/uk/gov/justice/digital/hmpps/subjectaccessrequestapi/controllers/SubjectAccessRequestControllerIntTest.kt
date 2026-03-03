package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.CreateSubjectAccessRequestEntity
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.SubjectAccessRequestRepository
import java.time.LocalDate

class SubjectAccessRequestControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var subjectAccessRequestRepository: SubjectAccessRequestRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  private val createSubjectAccessRequest = CreateSubjectAccessRequestEntity(
    nomisId = "A1111AA",
    ndeliusId = null,
    services = listOf("service1"),
    sarCaseReferenceNumber = "mockedCaseReference",
    dateTo = LocalDate.of(2022, 12, 25),
    dateFrom = LocalDate.of(2001, 1, 1),
  )

  @BeforeEach
  fun setup() {
    subjectAccessRequestRepository.deleteAll()
    serviceConfigurationRepository.deleteAll()
    serviceConfigurationRepository.saveAll(
      listOf(
        ServiceConfiguration(
          serviceName = "service1",
          label = "Service One",
          url = "http://service-one",
          enabled = true,
          templateMigrated = true,
          category = ServiceCategory.PRISON,
        ),
      ),
    )
  }

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User without ROLE_SAR_USER_ACCESS can't post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .returnResult()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can post subjectAccessRequest`() {
    webTestClient.post()
      .uri("/api/subjectAccessRequest")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .bodyValue(createSubjectAccessRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_USER_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_USER_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }

  @Test
  fun `User with ROLE_SAR_DATA_ACCESS can get subjectAccessRequests`() {
    webTestClient.get()
      .uri("/api/subjectAccessRequests")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
  }
}
