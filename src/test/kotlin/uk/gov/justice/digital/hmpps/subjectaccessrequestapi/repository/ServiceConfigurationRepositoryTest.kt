package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration

@DataJpaTest
class ServiceConfigurationRepositoryTest {

  @Autowired
  private lateinit var repository: ServiceConfigurationRepository

  @BeforeEach
  fun setup() {
    repository.deleteAll()
  }

  @Test
  fun `findByOrderByOrderAsc returns services in Ascending order`() {
    repository.saveAll(listOf(s2, s1, s3))

    val actual = repository.findByOrderByOrderAsc()

    assertThat(actual).isNotNull.isNotEmpty.hasSize(3).containsExactly(s1, s2, s3)
  }

  @Test
  fun `findAllByEnabledAndTemplateMigrated returns only enabled and migrated services`() {
    repository.saveAll(listOf(s2, s1, s3, s4))

    val actual = repository.findAllByEnabledAndTemplateMigrated()

    assertThat(actual).isNotNull.isNotEmpty.hasSize(1).containsExactly(s2)
  }

  @Test
  fun `findByServiceName returns the expected service when it exists`() {
    repository.saveAndFlush(s1)

    assertThat(repository.findByServiceName("service1")).isEqualTo(s1)
  }

  @Test
  fun `findByServiceName returns null when no service exists with requested serviceName`() {
    repository.saveAndFlush(s1)

    assertThat(repository.findByServiceName("serviceOne")).isNull()
  }

  companion object {

    private val s1 = ServiceConfiguration(
      serviceName = "service1",
      label = "Service One",
      url = "s1.com",
      order = 1,
      enabled = false,
      templateMigrated = false,
      category = PRISON,
    )

    private val s2 = ServiceConfiguration(
      serviceName = "service2",
      label = "Service Two",
      url = "s2.com",
      order = 2,
      enabled = true,
      templateMigrated = true,
      category = PRISON,
    )

    private val s3 = ServiceConfiguration(
      serviceName = "service3",
      label = "Service Three",
      url = "s3.com",
      order = 3,
      enabled = true,
      templateMigrated = false,
      category = PRISON,
    )

    private val s4 = ServiceConfiguration(
      serviceName = "service4",
      label = "Service Four",
      url = "s4.com",
      order = 4,
      enabled = false,
      templateMigrated = true,
      category = PRISON,
    )
  }
}
