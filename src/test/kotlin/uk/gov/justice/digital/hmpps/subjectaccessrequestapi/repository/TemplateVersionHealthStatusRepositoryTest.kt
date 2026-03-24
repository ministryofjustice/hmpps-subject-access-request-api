package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )
  private var serviceConfig2: ServiceConfiguration = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test002",
    label = "Test Zero Zero Two",
    url = "url",
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

  @Test
  fun `should update record to status HEALTHY when current status UNHEALTHY`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToHealthyWhereUnhealthy(
      serviceConfigurationId = serviceConfig1.id,
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(1)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = newModifiedTime,
        lastNotified = null,
      ),
    )
  }

  @Test
  fun `should set lastNotified to NULL when status changes from UNHEALTHY to HEALTHY`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
        lastNotified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToHealthyWhereUnhealthy(
      serviceConfigurationId = serviceConfig1.id,
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(1)
    val actual = templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)
    assertThat(actual).isNotNull
    assertThat(actual!!.lastNotified).isNull()
  }

  @Test
  fun `should not update status to HEALTHY when status is already HEALTHY`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToHealthyWhereUnhealthy(
      serviceConfigurationId = serviceConfig1.id,
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = savedTemplateVersionHealthStatus.lastModified,
        lastNotified = null,
      ),
    )
  }

  @Test
  fun `should not update to status HEALTHY when no matching service`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToHealthyWhereUnhealthy(
      serviceConfigurationId = UUID.randomUUID(),
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
        lastNotified = null,
      ),
    )
  }

  @Test
  fun `should update record to status UNHEALTHY when current status HEALTHY`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
        lastNotified = null,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToUnhealthyWhereHealthy(
      serviceConfigurationId = serviceConfig1.id,
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(1)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = newModifiedTime,
      ),
    )
  }

  @Test
  fun `should not update status to UNHEALTHY when status is already UNHEALTHY`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToUnhealthyWhereHealthy(
      serviceConfigurationId = serviceConfig1.id,
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = savedTemplateVersionHealthStatus.lastModified,
        lastNotified = null,
      ),
    )
  }

  @Test
  fun `should not update to status UNHEALTHY when no matching service`() {
    val savedTemplateVersionHealthStatus = templateVersionHealthStatusRepository.save(
      TemplateVersionHealthStatus(
        id = UUID.randomUUID(),
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
      ),
    )
    val newModifiedTime = Instant.parse("2099-11-04T14:33:45Z")

    val changedRecords = templateVersionHealthStatusRepository.updateStatusToUnhealthyWhereHealthy(
      serviceConfigurationId = UUID.randomUUID(),
      currentTime = newModifiedTime,
    )

    assertThat(changedRecords).isEqualTo(0)
    assertThat(templateVersionHealthStatusRepository.findByIdOrNull(savedTemplateVersionHealthStatus.id)).isEqualTo(
      TemplateVersionHealthStatus(
        id = savedTemplateVersionHealthStatus.id,
        status = HealthStatusType.HEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now,
        lastNotified = null,
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
