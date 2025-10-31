package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
class TemplateVersionRepositoryTest @Autowired constructor(
  val templateVersionRepository: TemplateVersionRepository,
  val serviceConfigurationRepository: ServiceConfigurationRepository,
) {

  private var serviceConfig1 = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "hmpps-example-service-1",
    label = "HMPPS Example Service One",
    url = "http://localhost:8080/",
    order = 999,
    enabled = true,
    templateMigrated = true,
  )

  private var serviceConfig2 = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "hmpps-example-service-2",
    label = "HMPPS Example Service Two",
    url = "http://localhost:8080/",
    order = 6666,
    enabled = true,
    templateMigrated = true,
  )

  private val service1TemplateV1Published = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig1,
    status = TemplateVersionStatus.PUBLISHED,
    version = 1,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v1-hash",
  )

  private val service1TemplateV2Pending = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig1,
    status = TemplateVersionStatus.PENDING,
    version = 2,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v1-hash",
  )

  private val service2TemplateV1Pending = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig2,
    status = TemplateVersionStatus.PENDING,
    version = 1,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v1-hash",
  )

  @BeforeEach
  fun setup() {
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.saveAll(listOf(serviceConfig1, serviceConfig2))

    insertTemplateVersions(
      service1TemplateV1Published,
      service1TemplateV2Pending,
      service2TemplateV1Pending,
    )
  }

  @AfterEach
  fun tearDown() {
    templateVersionRepository.deleteAll()
    serviceConfigurationRepository.deleteById(serviceConfig1.id)
    serviceConfigurationRepository.deleteById(serviceConfig2.id)
  }

  @Nested
  inner class DeleteByServiceConfigurationIdAndStatus {

    @Test
    fun `should delete all by status and service configuration ID`() {
      templateVersionRepository.deleteByServiceConfigurationIdAndStatus(
        id = serviceConfig1.id,
        status = TemplateVersionStatus.PENDING,
      )

      assertThat(templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfig1.id))
        .containsExactly(service1TemplateV1Published)
    }
  }

  @Nested
  inner class FindByServiceConfigurationIdOrderByVersionDesc {

    @Test
    fun `should return expected templates for service configuration ID`() {
      val actual = templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfig1.id)
      assertThat(actual).containsExactly(service1TemplateV2Pending, service1TemplateV1Published)
    }
  }

  @Nested
  inner class FindLatestByServiceConfigurationId {

    @Test
    fun `should return expected latest template version`() {
      val actual = templateVersionRepository.findLatestByServiceConfigurationId(serviceConfig1.id)
      assertThat(actual).isEqualTo(service1TemplateV2Pending)
    }
  }

  private fun insertTemplateVersions(vararg templateVersions: TemplateVersion) {
    templateVersionRepository.saveAll(templateVersions.toList())

    assertThat(templateVersionRepository.findAll())
      .containsAll(templateVersions.asList())
  }
}
