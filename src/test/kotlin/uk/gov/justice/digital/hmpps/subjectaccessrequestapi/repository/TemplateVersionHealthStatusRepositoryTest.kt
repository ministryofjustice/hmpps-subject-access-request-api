package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
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

  private var serviceConfig1: ServiceConfiguration = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test001",
    label = "Test Zero Zero One",
    url = "url",
    order = 999,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )
  private var serviceConfig2: ServiceConfiguration = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test002",
    label = "Test Zero Zero Two",
    url = "url",
    order = 1000,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val templateVersionHealthStatus = TemplateVersionHealthStatus(
    id = UUID.randomUUID(),
    status = HealthStatusType.UNHEALTHY,
    serviceConfiguration = serviceConfig1,
    lastModified = now,
  )

  @BeforeEach
  fun setup() {
    templateVersionHealthStatusRepository.deleteAll()
    serviceConfigurationRepository.saveAllAndFlush(listOf(serviceConfig1, serviceConfig2))
  }

  @AfterEach
  fun cleanUp() {
    templateVersionHealthStatusRepository.deleteAll()
    serviceConfigurationRepository.delete(serviceConfig1)
  }

  @Test
  fun `should save new record`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)

    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(templateVersionHealthStatus.id))
      .isEqualTo(templateVersionHealthStatus)
  }

  @Test
  fun `delete should not delete service configuration record`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)
    assertThat(serviceConfigurationRepository.findByIdOrNull(serviceConfig1.id)).isNotNull
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(templateVersionHealthStatus.id)).isNotNull

    templateVersionHealthStatusRepository.deleteById(templateVersionHealthStatus.id)

    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(templateVersionHealthStatus.id)).isNull()
    assertThat(serviceConfigurationRepository.findByIdOrNull(serviceConfig1.id)).isNotNull
  }

  @Test
  fun `should retrieve record by service configuration id when exists`() {
    templateVersionHealthStatusRepository.save(templateVersionHealthStatus)

    assertThat(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfig1.id))
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
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords =
      templateVersionHealthStatusRepository.updateStatusWhenChanged(serviceConfig1.id, newStatus, newModifiedTime)

    assertThat(changedRecords).isEqualTo(1)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = newStatus,
        serviceConfiguration = serviceConfig1,
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
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords =
      templateVersionHealthStatusRepository.updateStatusWhenChanged(serviceConfig1.id, newStatus, newModifiedTime)

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = existingStatus,
        serviceConfiguration = serviceConfig1,
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
        serviceConfiguration = serviceConfig1,
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
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
  }

  @Nested
  inner class FindUnhealthyTemplates {

    @Test
    fun `should return empty when no unhealth templates found`() {
      assertThat(
        templateVersionHealthStatusRepository.findUnhealthyTemplates(
          unhealthyStatusThreshold = now,
          lastNotifiedThreshold = now,
        ),
      ).isEmpty()
    }

    @Test
    fun `should return empty when unhealthy template is inside the unhealthy status threshold`() {
      templateVersionHealthStatusRepository.saveAndFlush(
        TemplateVersionHealthStatus(
          status = HealthStatusType.UNHEALTHY,
          serviceConfiguration = serviceConfig1,
          lastModified = now.minusMinutes(20),
          lastNotified = null,
        ),
      )

      assertThat(
        templateVersionHealthStatusRepository.findUnhealthyTemplates(
          unhealthyStatusThreshold = now.minusMinutes(30),
          lastNotifiedThreshold = now.minusMinutes(60),
        ),
      ).isEmpty()
    }

    @Test
    fun `should return result when unhealthy template is outside the unhealthy status threshold and last notified threshold is null`() {
      val expected = templateVersionHealthStatusRepository.saveAndFlush(
        TemplateVersionHealthStatus(
          status = HealthStatusType.UNHEALTHY,
          serviceConfiguration = serviceConfig1,
          lastModified = now.minusMinutes(31),
          lastNotified = null,
        ),
      )

      val actual = templateVersionHealthStatusRepository.findUnhealthyTemplates(
        unhealthyStatusThreshold = now.minusMinutes(30),
        lastNotifiedThreshold = now.minusMinutes(60),
      )

      assertThat(actual).hasSize(1)
      assertThat(actual[0]).isEqualTo(expected)
    }

    @Test
    fun `should return result when unhealthy template is outside the unhealthy status threshold and last notified outside threshold`() {
      val expected = templateVersionHealthStatusRepository.saveAndFlush(
        TemplateVersionHealthStatus(
          status = HealthStatusType.UNHEALTHY,
          serviceConfiguration = serviceConfig1,
          lastModified = now.minusMinutes(31),
          lastNotified = now.minusMinutes(61),
        ),
      )

      val actual = templateVersionHealthStatusRepository.findUnhealthyTemplates(
        unhealthyStatusThreshold = now.minusMinutes(30),
        lastNotifiedThreshold = now.minusMinutes(60),
      )

      assertThat(actual).hasSize(1)
      assertThat(actual[0]).isEqualTo(expected)
    }

    @Test
    fun `should return empty when healthy template is outside the unhealthy status threshold and last notified outside threshold`() {
      // Scenario should never happen and updating status to HEALTHY should set last notified to NULL.
      // Test proves query only finds results where status is UNHEALTHY
      templateVersionHealthStatusRepository.saveAndFlush(
        TemplateVersionHealthStatus(
          status = HealthStatusType.HEALTHY,
          serviceConfiguration = serviceConfig1,
          lastModified = now.minusMinutes(31),
          lastNotified = now.minusMinutes(61),
        ),
      )

      assertThat(
        templateVersionHealthStatusRepository.findUnhealthyTemplates(
          unhealthyStatusThreshold = now.minusMinutes(30),
          lastNotifiedThreshold = now.minusMinutes(60),
        ),
      ).isEmpty()
    }
  }

  @Nested
  inner class FindByServiceConfigurationIds {

    @Test
    fun `should return empty list when IDs do not match any entries`() {
      assertThat(
        templateVersionHealthStatusRepository.findByServiceConfigurationIds(
          listOf(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
          ),
        ),
      ).isEmpty()
    }

    @Test
    fun `should return all expected templateHealthStatuses matching service configuration ID`() {
      templateVersionHealthStatusRepository.deleteAll()

      val t1 = TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      )

      val t2 = TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig2,
        lastModified = now,
      )

      templateVersionHealthStatusRepository.saveAllAndFlush(listOf(t1, t2))

      val actual = templateVersionHealthStatusRepository.findByServiceConfigurationIds(
        listOf(
          serviceConfig1.id,
          serviceConfig2.id,
        ),
      )

      assertThat(actual).hasSize(2)
      assertThat(actual).containsExactlyInAnyOrder(t1, t2)
    }

    @Test
    fun `should return only templateHealthStatuses matching the provided service configuration IDs`() {
      templateVersionHealthStatusRepository.deleteAll()

      val t1 = TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      )

      val t2 = TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig2,
        lastModified = now,
      )

      templateVersionHealthStatusRepository.saveAllAndFlush(listOf(t1, t2))

      val actual = templateVersionHealthStatusRepository.findByServiceConfigurationIds(listOf(serviceConfig1.id))
      assertThat(actual).hasSize(1)
      assertThat(actual).containsExactlyInAnyOrder(t1)
    }
  }

  private fun Instant.minusMinutes(
    minutes: Int,
  ): Instant = this.minus(minutes.toLong(), ChronoUnit.MINUTES)
}
