package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@DataJpaTest
class TemplateVersionHealthStatusRepositoryTest {

  @Autowired
  private lateinit var templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  private val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

  private var serviceConfig: ServiceConfiguration = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test001",
    label = "Test Zero Zero One",
    url = "url",
    order = 999,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val templateVersionHealthStatus = TemplateVersionHealthStatus(
    id = UUID.randomUUID(),
    status = HealthStatusType.UNHEALTHY,
    serviceConfiguration = serviceConfig,
    lastModified = now,
  )

  @BeforeEach
  fun setup() {
    serviceConfigurationRepository.save(serviceConfig)
  }

  @AfterEach
  fun cleanUp() {
    serviceConfigurationRepository.delete(serviceConfig)
  }

  @Test
  fun `should save new record`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)

    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(templateVersionHealthStatus.id))
      .isEqualTo(templateVersionHealthStatus)
  }

  @Test
  fun `should retrieve record by service configuration id when exists`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)

    assertThat(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfig.id))
      .isEqualTo(templateVersionHealthStatus)
  }

  @Test
  fun `should not retrieve record by service configuration id when does not exist`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)

    assertThat(templateVersionHealthStatusRepository.findByServiceConfigurationId(UUID.randomUUID()))
      .isNull()
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "HEALTHY, UNHEALTHY",
      "UNHEALTHY, HEALTHY",
    ],
  )
  fun `should update record when change in status`(existingStatus: HealthStatusType, newStatus: HealthStatusType) {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = existingStatus,
        serviceConfiguration = serviceConfig,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords =
      templateVersionHealthStatusRepository.updateStatusWhenChanged(serviceConfig.id, newStatus, newModifiedTime)

    assertThat(changedRecords).isEqualTo(1)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = newStatus,
        serviceConfiguration = serviceConfig,
        lastModified = newModifiedTime,
      ),
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "HEALTHY, HEALTHY",
      "UNHEALTHY, UNHEALTHY",
    ],
  )
  fun `should not update record when no change in status`(
    existingStatus: HealthStatusType,
    newStatus: HealthStatusType,
  ) {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = existingStatus,
        serviceConfiguration = serviceConfig,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords =
      templateVersionHealthStatusRepository.updateStatusWhenChanged(serviceConfig.id, newStatus, newModifiedTime)

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = existingStatus,
        serviceConfiguration = serviceConfig,
        lastModified = now,
      ),
    )
  }

  @Test
  fun `should not update record when no matching service`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords =
      templateVersionHealthStatusRepository.updateStatusWhenChanged(
        UUID.randomUUID(),
        HealthStatusType.UNHEALTHY,
        newModifiedTime,
      )

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig,
        lastModified = now,
      ),
    )
  }
}
