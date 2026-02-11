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

@DataJpaTest
class ServiceConfigurationRepositoryTest {

  @Autowired
  private lateinit var repository: ServiceConfigurationRepository

  @BeforeEach
  fun setup() {
    repository.deleteAll()
  }

  @Nested
  inner class FindAllOrderedByReportFormat {

    @Test
    fun `should return empty list when no services exist`() {
      assertThat(repository.findAllReportOrdering()).isEmpty()
    }

    @Test
    fun `should return services in expected order when no G services exist`() {
      repository.saveAllAndFlush(listOf(probation2, prison1, prison2, prison3, prison4, probation1))

      val actual = repository.findAllReportOrdering()
      assertThat(actual).hasSize(6)

      assertThat(actual).containsExactly(
        prison1,
        prison2,
        prison3,
        prison4,
        probation1,
        probation2,
      )
    }

    @Test
    fun `should return services in expected order when no Prison services exist`() {
      repository.saveAllAndFlush(listOf(probation2, sG1, probation1, sG2))

      val actual = repository.findAllReportOrdering()
      assertThat(actual).hasSize(4)

      assertThat(actual).containsExactly(
        sG1,
        sG2,
        probation1,
        probation2,
      )
    }

    @Test
    fun `should return services in expected order when no Probation services exist`() {
      repository.saveAllAndFlush(listOf(sG1, prison1, prison2, sG2, prison3, prison4))

      val actual = repository.findAllReportOrdering()
      assertThat(actual).hasSize(6)

      assertThat(actual).containsExactly(
        sG1,
        sG2,
        prison1,
        prison2,
        prison3,
        prison4,
      )
    }

    @Test
    fun `should return services in expected order`() {
      repository.saveAllAndFlush(listOf(sG1, prison1, prison2, probation2, prison3, prison4, sG2, probation1))

      val actual = repository.findAllReportOrdering()
      assertThat(actual).hasSize(8)

      // Expected order is
      // G services sorted alphabetically,
      // Prison services sorted alphabetically,
      // Probation sorted services alphabetically
      assertThat(actual).containsExactly(
        sG1,
        sG2,
        prison1,
        prison2,
        prison3,
        prison4,
        probation1,
        probation2,
      )
    }
  }

  @Test
  fun `findAllByEnabledAndTemplateMigrated returns only enabled and migrated services`() {
    repository.saveAll(listOf(prison2, prison1, prison3, prison4))

    val actual = repository.findAllByEnabledAndTemplateMigrated()

    assertThat(actual).isNotNull.isNotEmpty.hasSize(1).containsExactly(prison2)
  }

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
