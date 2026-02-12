package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository

class ServiceConfigurationServiceTest {

  private val s1 = ServiceConfiguration(serviceName = "service1", label = "Service 1", url = "s1.com", enabled = true, templateMigrated = false, category = PRISON)
  private val s2 = ServiceConfiguration(serviceName = "service2", label = "Service 2", url = "s2.com", enabled = true, templateMigrated = false, category = PRISON)
  private val s3 = ServiceConfiguration(serviceName = "service3", label = "Service 3", url = "s3.com", enabled = true, templateMigrated = false, category = PRISON)
  private val s4 = ServiceConfiguration(serviceName = "service4", label = "Service 4", url = "s4.com", enabled = true, templateMigrated = false, category = PROBATION)
  private val s5 = ServiceConfiguration(serviceName = "service5", label = "Service 5", url = "s4.com", enabled = true, templateMigrated = false, category = PROBATION)
  private val g1 = ServiceConfiguration(serviceName = "G1", label = "G1", url = "G1", enabled = true, templateMigrated = false, category = PRISON)
  private val g2 = ServiceConfiguration(serviceName = "G2", label = "G2", url = "G2", enabled = true, templateMigrated = false, category = PRISON)
  private val g3 = ServiceConfiguration(serviceName = "G3", label = "G3", url = "G3", enabled = true, templateMigrated = false, category = PRISON)

  private val serviceConfigurationRepository: ServiceConfigurationRepository = mock()
  private val service = ServiceConfigurationService(serviceConfigurationRepository)

  @Test
  fun `should return empty list when no results are returned`() {
    whenever(serviceConfigurationRepository.findAll())
      .thenReturn(emptyList())

    assertThat(service.getServiceConfigurationSanitised()).isEmpty()
    verify(serviceConfigurationRepository, times(1)).findAll()
  }

  @Test
  fun `should return expected list of service configuration in the correct ordering`() {
    whenever(serviceConfigurationRepository.findAll())
      .thenReturn(listOf(s5, s4, g1, g3, s1, g2, s2, s3))

    assertThat(service.getServiceConfigurationSanitised())
      .containsExactlyElementsOf(listOf(g1, g2, g3, s1, s2, s3, s4, s5))

    verify(serviceConfigurationRepository, times(1)).findAll()
  }

  @Test
  fun `should return expected service by service service name`() {
    whenever(serviceConfigurationRepository.findByServiceName("service1"))
      .thenReturn(s1)

    assertThat(service.getByServiceName("service1")).isEqualTo(s1)

    verify(serviceConfigurationRepository, times(1))
      .findByServiceName("service1")
  }
}
