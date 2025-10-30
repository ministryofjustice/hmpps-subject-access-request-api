package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository

class ServiceConfigurationServiceTest {

  private val s1 = ServiceConfiguration(serviceName = "service1", label = "Service One", url = "s1.com", order = 1, enabled = true, templateMigrated = false)
  private val s2 = ServiceConfiguration(serviceName = "service2", label = "Service Two", url = "s2.com", order = 2, enabled = true, templateMigrated = false)
  private val s3 = ServiceConfiguration(serviceName = "service3", label = "Service Three", url = "s3.com", order = 3, enabled = true, templateMigrated = false)
  private val g1 = ServiceConfiguration(serviceName = "G1", label = "G1", url = "G1", order = 4, enabled = true, templateMigrated = false)
  private val g2 = ServiceConfiguration(serviceName = "G2", label = "G2", url = "G2", order = 5, enabled = true, templateMigrated = false)
  private val g3 = ServiceConfiguration(serviceName = "G3", label = "G3", url = "G3", order = 5, enabled = true, templateMigrated = false)

  private val serviceConfigurationRepository: ServiceConfigurationRepository = mock()
  private val service = ServiceConfigurationService(serviceConfigurationRepository)

  @Test
  fun `should return null when serviceConfigurationRepository return null`() {
    whenever(serviceConfigurationRepository.findByOrderByOrderAsc())
      .thenReturn(null)

    assertThat(service.getServiceConfigurationSanitised()).isNull()
    verify(serviceConfigurationRepository, times(1)).findByOrderByOrderAsc()
  }

  @Test
  fun `should return empty list when no results are returned`() {
    whenever(serviceConfigurationRepository.findByOrderByOrderAsc())
      .thenReturn(emptyList())

    assertThat(service.getServiceConfigurationSanitised()).isEmpty()
    verify(serviceConfigurationRepository, times(1)).findByOrderByOrderAsc()
  }

  @Test
  fun `should return expected list of service configuration`() {
    whenever(serviceConfigurationRepository.findByOrderByOrderAsc())
      .thenReturn(listOf(s1, s2, s3, g1, g2, g3))

    assertThat(service.getServiceConfigurationSanitised())
      .containsExactlyElementsOf(listOf(s1, s2, s3, g1, g2, g3))

    verify(serviceConfigurationRepository, times(1)).findByOrderByOrderAsc()
  }
}
