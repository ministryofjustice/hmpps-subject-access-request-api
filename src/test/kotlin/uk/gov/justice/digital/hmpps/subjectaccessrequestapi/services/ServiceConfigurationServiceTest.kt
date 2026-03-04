package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.ServiceConfigurationNotFoundException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PRISON
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory.PROBATION
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

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

  @Nested
  inner class GetServiceConfigurationSanitised {

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
  }

  @Nested
  inner class GetByServiceName {

    @Test
    fun `should return expected service by service service name`() {
      whenever(serviceConfigurationRepository.findByServiceName("service1"))
        .thenReturn(s1)

      assertThat(service.getByServiceName("service1")).isEqualTo(s1)

      verify(serviceConfigurationRepository, times(1))
        .findByServiceName("service1")
    }
  }

  @Nested
  inner class CreateServiceConfiguration {

    @Test
    fun `should throw exception when creating a new service with a service name that already exists`() {
      whenever(serviceConfigurationRepository.findByServiceName("service1"))
        .thenReturn(s1)

      val actual = assertThrows<ValidationException> {
        service.createServiceConfiguration(
          ServiceConfiguration(
            serviceName = "service1",
            label = "Service 1",
            url = "s1.com",
            enabled = true,
            templateMigrated = false,
            category = PRISON,
          ),
        )
      }
      assertThat(actual.message).isEqualTo("Service configuration with name service1 already exists")
    }

    @Test
    fun `should create a new service when no service exists with service name`() {
      val newServiceConfig = ServiceConfiguration(
        serviceName = "service1",
        label = "Service 1",
        url = "s1.com",
        enabled = true,
        templateMigrated = false,
        category = PRISON,
      )

      whenever(serviceConfigurationRepository.findByServiceName("service1"))
        .thenReturn(null)

      whenever(serviceConfigurationRepository.saveAndFlush(newServiceConfig))
        .thenReturn(newServiceConfig)

      val captor = argumentCaptor<ServiceConfiguration>()

      service.createServiceConfiguration(newServiceConfig)

      verify(serviceConfigurationRepository, times(1))
        .findByServiceName("service1")
      verify(serviceConfigurationRepository, times(1))
        .saveAndFlush(captor.capture())

      assertThat(captor.allValues).hasSize(1)
      assertThat(captor.firstValue).isEqualTo(newServiceConfig)
    }
  }

  @Nested
  inner class DeleteByServiceName {

    @Test
    fun `should delete service configuration by service name`() {
      service.deleteByServiceName("service1")

      verify(serviceConfigurationRepository, times(1)).deleteByServiceName("service1")
    }
  }

  @Nested
  inner class UpdateServiceConfiguration {
    private val update = ServiceConfigurationService.ServiceConfigurationUpdate(
      id = UUID.randomUUID(),
      serviceName = "X",
      label = "Y",
      url = "Z",
      enabled = false,
      templateMigrated = false,
      category = PROBATION,
    )

    @Test
    fun `should throw exception when another service configuration exists with the supplied service name`() {
      val update = ServiceConfigurationService.ServiceConfigurationUpdate(
        id = UUID.randomUUID(),
        serviceName = "service1",
        label = "Y",
        url = "Z",
        enabled = false,
        templateMigrated = false,
        category = PROBATION,
      )

      whenever(serviceConfigurationRepository.findByServiceNameAndIdNot(update.serviceName, update.id))
        .thenReturn(s1)

      val actual = assertThrows<ValidationException> { service.updateServiceConfiguration(update) }
      assertThat(actual.message).isEqualTo("serviceName 'service1' is already in use by Service configuration ${s1.id}")

      verify(serviceConfigurationRepository, times(1)).findByServiceNameAndIdNot(update.serviceName, update.id)
      verifyNoMoreInteractions(serviceConfigurationRepository)
    }

    @Test
    fun `should throw Service Configuration Not Found Exception when findById returns null`() {
      whenever(serviceConfigurationRepository.findByServiceNameAndIdNot(update.serviceName, update.id))
        .thenReturn(null)
      whenever(serviceConfigurationRepository.findById(update.id))
        .thenReturn(Optional.empty<ServiceConfiguration>())

      val actual = assertThrows<ServiceConfigurationNotFoundException> { service.updateServiceConfiguration(update) }
      assertThat(actual.message).isEqualTo("Service configuration service not found for id: ${update.id}")

      verify(serviceConfigurationRepository, times(1)).findByServiceNameAndIdNot(update.serviceName, update.id)
      verify(serviceConfigurationRepository, times(1)).findById(update.id)
      verifyNoMoreInteractions(serviceConfigurationRepository)
    }

    @Test
    fun `should update service configuration`() {
      whenever(serviceConfigurationRepository.findByServiceNameAndIdNot(update.serviceName, update.id))
        .thenReturn(null)
      whenever(serviceConfigurationRepository.findById(update.id))
        .thenReturn(Optional.of(s1))
      whenever(serviceConfigurationRepository.saveAndFlush(any<ServiceConfiguration>()))
        .thenReturn(mock<ServiceConfiguration>())

      val captor = argumentCaptor<ServiceConfiguration>()

      service.updateServiceConfiguration(update)

      verify(serviceConfigurationRepository, times(1)).findByServiceNameAndIdNot(update.serviceName, update.id)
      verify(serviceConfigurationRepository, times(1)).findById(update.id)
      verify(serviceConfigurationRepository, times(1)).saveAndFlush(captor.capture())

      assertThat(captor.allValues).hasSize(1)
      val actual = captor.firstValue
      assertThat(actual.serviceName).isEqualTo("X")
      assertThat(actual.label).isEqualTo("Y")
      assertThat(actual.url).isEqualTo("Z")
      assertThat(actual.category).isEqualTo(PROBATION)
      assertThat(actual.enabled).isFalse()
      assertThat(actual.templateMigrated).isFalse()
    }
  }

  @Nested
  inner class UpdateSuspended {

    private val s = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "service1",
      label = "Service One",
      url = "www.s1.com",
      enabled = true,
      templateMigrated = false,
      category = PROBATION,
      suspended = true,
      suspendedAt = Instant.now(),
    )

    @Test
    fun `should throw exception when service not found`() {
      whenever(serviceConfigurationRepository.findById(s.id)).thenReturn(Optional.empty())

      val actual = assertThrows<ServiceConfigurationNotFoundException> { service.updateSuspended(s.id, true) }

      assertThat(actual.message).isEqualTo("Service configuration service not found for id: ${s.id}")

      verify(serviceConfigurationRepository, times(1)).findById(s.id)
      verifyNoMoreInteractions(serviceConfigurationRepository)
    }

    @Test
    fun `should set suspended true and set suspended at`() {
      whenever(serviceConfigurationRepository.findById(s.id)).thenReturn(Optional.of(s))
      whenever(serviceConfigurationRepository.saveAndFlush(s)).thenAnswer { answer -> answer.arguments[0] }

      val captor = argumentCaptor<ServiceConfiguration>()
      val start = Instant.now()

      assertThat(service.updateSuspended(s.id, true)).isNotNull

      verify(serviceConfigurationRepository, times(1)).findById(s.id)
      verify(serviceConfigurationRepository, times(1)).saveAndFlush(captor.capture())

      assertThat(captor.allValues).hasSize(1)
      val actual = captor.firstValue
      assertThat(actual.serviceName).isEqualTo("service1")
      assertThat(actual.label).isEqualTo("Service One")
      assertThat(actual.url).isEqualTo("www.s1.com")
      assertThat(actual.category).isEqualTo(PROBATION)
      assertThat(actual.enabled).isTrue()
      assertThat(actual.templateMigrated).isFalse
      assertThat(actual.suspended).isTrue
      assertThat(actual.suspendedAt).isNotNull
      assertThat(actual.suspendedAt).isBetween(start, Instant.now())
    }

    @Test
    fun `should set suspended false and suspended at null`() {
      whenever(serviceConfigurationRepository.findById(s.id)).thenReturn(Optional.of(s))
      whenever(serviceConfigurationRepository.saveAndFlush(s)).thenAnswer { answer -> answer.arguments[0] }

      val captor = argumentCaptor<ServiceConfiguration>()

      assertThat(service.updateSuspended(s.id, false)).isNotNull

      verify(serviceConfigurationRepository, times(1)).findById(s.id)
      verify(serviceConfigurationRepository, times(1)).saveAndFlush(captor.capture())

      assertThat(captor.allValues).hasSize(1)
      val actual = captor.firstValue
      assertThat(actual.serviceName).isEqualTo("service1")
      assertThat(actual.label).isEqualTo("Service One")
      assertThat(actual.url).isEqualTo("www.s1.com")
      assertThat(actual.category).isEqualTo(PROBATION)
      assertThat(actual.enabled).isTrue()
      assertThat(actual.templateMigrated).isFalse
      assertThat(actual.suspended).isFalse
      assertThat(actual.suspendedAt).isNull()
    }

    @Test
    fun `should update suspended at when service already suspended`() {
      val originalSuspendedAt = Instant.now()
      s.apply {
        suspended = true
        suspendedAt = originalSuspendedAt
      }

      whenever(serviceConfigurationRepository.findById(s.id)).thenReturn(Optional.of(s))
      whenever(serviceConfigurationRepository.saveAndFlush(s)).thenAnswer { answer -> answer.arguments[0] }

      val captor = argumentCaptor<ServiceConfiguration>()
      val start = Instant.now()

      assertThat(service.updateSuspended(s.id, true)).isNotNull

      verify(serviceConfigurationRepository, times(1)).findById(s.id)
      verify(serviceConfigurationRepository, times(1)).saveAndFlush(captor.capture())

      assertThat(captor.allValues).hasSize(1)
      val actual = captor.firstValue
      assertThat(actual.serviceName).isEqualTo("service1")
      assertThat(actual.label).isEqualTo("Service One")
      assertThat(actual.url).isEqualTo("www.s1.com")
      assertThat(actual.category).isEqualTo(PROBATION)
      assertThat(actual.enabled).isTrue()
      assertThat(actual.templateMigrated).isFalse
      assertThat(actual.suspended).isTrue
      assertThat(actual.suspendedAt).isNotNull()
      assertThat(actual.suspendedAt).isBetween(start, Instant.now())
    }
  }
}
