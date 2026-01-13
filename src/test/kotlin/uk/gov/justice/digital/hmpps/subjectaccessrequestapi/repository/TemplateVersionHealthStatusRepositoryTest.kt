package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
class TemplateVersionHealthStatusRepositoryTest {

  @Autowired
  private lateinit var templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  private val now = LocalDateTime.now()

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
}
