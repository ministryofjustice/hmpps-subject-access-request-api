package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import java.util.UUID

@DataJpaTest
class ServiceConfigurationRepositoryTest {

  @Autowired
  private lateinit var repository: ServiceConfigurationRepository

  @BeforeEach
  fun setup() {
    repository.deleteAll()
  }

  @Nested
  inner class FindAllByEnabledAndTemplateMigrated {

    @Test
    fun `findAllByEnabledAndTemplateMigrated returns only enabled and migrated services`() {
      repository.saveAll(listOf(prison2, prison1, prison3, prison4))

      val actual = repository.findAllByEnabledAndTemplateMigrated()

      assertThat(actual).isNotNull.isNotEmpty.hasSize(1).containsExactly(prison2)
    }
  }

  @Nested
  inner class FindByServiceName {

    @Test
    fun `findByServiceName returns the expected service when it exists`() {
      repository.saveAndFlush(prison1)

      assertThat(repository.findByServiceName("service1")).isEqualTo(prison1)
    }

    @Test
    fun `findByServiceName returns null when no service exists with requested serviceName`() {
      repository.saveAndFlush(prison1)

      assertThat(repository.findByServiceName("serviceOne")).isNull()
    }
  }

  @Nested
  inner class DeleteByServiceName {

    @Test
    fun `deleteByServiceName should delete expected records`() {
      repository.saveAllAndFlush(listOf(prison1, prison2))

      repository.deleteByServiceName("service2")

      assertThat(repository.findByServiceName("service2")).isNull()
      assertThat(repository.findByServiceName("service1")).isEqualTo(prison1)
    }
  }

  @Nested
  inner class FindByServiceNameAndIdNot {

    @Test
    fun `findByServiceNameAndIdNot should return null when no service configuration with the specified serviceName exists`() {
      assertThat(repository.findByServiceNameAndIdNot("service999", UUID.randomUUID())).isNull()
    }

    @Test
    fun `findByServiceNameAndIdNot should return service config when service name matches but ID is different`() {
      repository.saveAndFlush(prison1)
      assertThat(repository.findByServiceNameAndIdNot("service1", UUID.randomUUID())).isEqualTo(prison1)
    }

    @Test
    fun `findByServiceNameAndIdNot should return null when a service config exists with the request service name matches and the ID the same`() {
      repository.saveAndFlush(prison1)
      assertThat(repository.findByServiceNameAndIdNot("service1", prison1.id)).isNull()
    }
  }

  companion object {

    private val prison1 = ServiceConfiguration(
      serviceName = "service1",
      label = "Prison Service 1",
      url = "s1.com",
      enabled = false,
      templateMigrated = false,
      category = PRISON,
    )

    private val prison2 = ServiceConfiguration(
      serviceName = "service2",
      label = "Prison Service 2",
      url = "s2.com",
      enabled = true,
      templateMigrated = true,
      category = PRISON,
    )

    private val prison3 = ServiceConfiguration(
      serviceName = "service3",
      label = "Prison Service 3",
      url = "s3.com",
      enabled = true,
      templateMigrated = false,
      category = PRISON,
    )

    private val prison4 = ServiceConfiguration(
      serviceName = "service4",
      label = "Prison Service 4",
      url = "s4.com",
      enabled = false,
      templateMigrated = true,
      category = PRISON,
    )

    private val probation1 = ServiceConfiguration(
      serviceName = "service5",
      label = "Probation Service 1",
      url = "s5.com",
      enabled = false,
      templateMigrated = true,
      category = PROBATION,
    )

    private val probation2 = ServiceConfiguration(
      serviceName = "service6",
      label = "Probation Service 2",
      url = "s6.com",
      enabled = false,
      templateMigrated = true,
      category = PROBATION,
    )

    private val sG1 = ServiceConfiguration(
      serviceName = "G1",
      label = "G1",
      url = "G1.com",
      enabled = false,
      templateMigrated = true,
      category = PROBATION,
    )

    private val sG2 = ServiceConfiguration(
      serviceName = "G2",
      label = "G2",
      url = "G2.com",
      enabled = false,
      templateMigrated = true,
      category = PRISON,
    )
  }
}
