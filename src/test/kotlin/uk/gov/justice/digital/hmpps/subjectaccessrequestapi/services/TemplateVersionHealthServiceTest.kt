package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.DynamicServicesClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionHealthStatusRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

private const val TEMPLATE_ONE = "<h1>My Template One</h1>"

class TemplateVersionHealthServiceTest {

  companion object {
    private val NOW: Instant = Instant.now()

    private val serviceConfig1 = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "test1",
      label = "Test One",
      url = "url",
      order = 123,
      enabled = true,
      templateMigrated = true,
      category = ServiceCategory.PRISON,
    )

    private val serviceConfig2 = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "test2",
      label = "Test Two",
      url = "url",
      order = 456,
      enabled = true,
      templateMigrated = true,
      category = ServiceCategory.PRISON,
    )
  }

  private val templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository = mock()
  private val templateVersionService: TemplateVersionService = mock()
  private val dynamicServicesClient: DynamicServicesClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

  private val updateTemplateVersionHealthService = TemplateVersionHealthService(
    templateVersionHealthStatusRepository,
    templateVersionService,
    dynamicServicesClient,
    clock,
    telemetryClient,
    10L,
    10L,
  )

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(templateVersionHealthStatusRepository)
  }

  @Test
  fun `should not update status when template not found`() {
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfig1)).thenReturn(null)

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfig1)

    verifyNoInteractions(templateVersionHealthStatusRepository)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "true, HEALTHY",
      "false, UNHEALTHY",
    ],
  )
  fun `should create status when template found and no existing health status`(
    fileHashValid: Boolean,
    expectedHealthStatus: HealthStatusType,
  ) {
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfig1)).thenReturn(TEMPLATE_ONE)
    whenever(
      templateVersionService.isTemplateHashValid(
        serviceConfig1.id,
        "0a1bf7a8b5b414b50e3b9a0c746e1b493dadddaf74ba3861ff4f663ea65938d2",
      ),
    ).thenReturn(fileHashValid)
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfig1.id))
      .thenReturn(null)

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfig1)

    verify(templateVersionHealthStatusRepository).findByServiceConfigurationId(serviceConfig1.id)
    verify(templateVersionHealthStatusRepository).save(
      argThat { templateVersionHealth ->
        templateVersionHealth.serviceConfiguration == serviceConfig1 &&
          templateVersionHealth.status == expectedHealthStatus &&
          templateVersionHealth.lastModified.toEpochMilli() == NOW.toEpochMilli()
      },
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "true, HEALTHY",
      "false, UNHEALTHY",
    ],
  )
  fun `should update status when template found and existing health status`(
    fileHashValid: Boolean,
    expectedHealthStatus: HealthStatusType,
  ) {
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfig1)).thenReturn(TEMPLATE_ONE)
    whenever(
      templateVersionService.isTemplateHashValid(
        serviceConfig1.id,
        "0a1bf7a8b5b414b50e3b9a0c746e1b493dadddaf74ba3861ff4f663ea65938d2",
      ),
    ).thenReturn(fileHashValid)
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfig1.id))
      .thenReturn(
        TemplateVersionHealthStatus(
          serviceConfiguration = serviceConfig1,
          lastModified = Instant.now(clock),
        ),
      )
    whenever(
      templateVersionHealthStatusRepository.updateStatusWhenChanged(
        serviceConfig1.id,
        expectedHealthStatus,
        NOW,
      ),
    ).thenReturn(1)

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfig1)

    verify(templateVersionHealthStatusRepository).findByServiceConfigurationId(serviceConfig1.id)
    verify(templateVersionHealthStatusRepository).updateStatusWhenChanged(
      serviceConfig1.id,
      expectedHealthStatus,
      NOW,
    )
  }

  @Test
  fun `should not create new record if one already exists for service configuration even if update is unsuccessful`() {
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfig1)).thenReturn(TEMPLATE_ONE)
    whenever(
      templateVersionService.isTemplateHashValid(
        serviceConfig1.id,
        "0a1bf7a8b5b414b50e3b9a0c746e1b493dadddaf74ba3861ff4f663ea65938d2",
      ),
    ).thenReturn(true)
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfig1.id))
      .thenReturn(
        TemplateVersionHealthStatus(
          serviceConfiguration = serviceConfig1,
          lastModified = Instant.now(clock),
        ),
      )
    whenever(
      templateVersionHealthStatusRepository.updateStatusWhenChanged(
        serviceConfig1.id,
        HealthStatusType.HEALTHY,
        NOW,
      ),
    ).thenReturn(0) // return 0 to indicate update not successful

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfig1)

    verify(templateVersionHealthStatusRepository).findByServiceConfigurationId(serviceConfig1.id)
    verify(templateVersionHealthStatusRepository).updateStatusWhenChanged(
      serviceConfig1.id,
      HealthStatusType.HEALTHY,
      NOW,
    )
    verify(templateVersionHealthStatusRepository, never())
      .save(any<TemplateVersionHealthStatus>())
  }

  @Test
  fun `getTemplateHealthStatusByServiceConfigurationIds should return empty map when not matches exist`() {
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationIds(any()))
      .thenReturn(emptyList())

    val inputIds = listOf(
      UUID.randomUUID(),
      UUID.randomUUID(),
    )
    assertThat(updateTemplateVersionHealthService.getTemplateHealthStatusByServiceConfigurationIds(inputIds)).isEmpty()

    verify(templateVersionHealthStatusRepository, times(1))
      .findByServiceConfigurationIds(inputIds)
  }

  @Test
  fun `getTemplateHealthStatusByServiceConfigurationIds should return expected map`() {
    val t1 = TemplateVersionHealthStatus(
      serviceConfiguration = serviceConfig1,
      status = HealthStatusType.UNHEALTHY,
    )
    val t2 = TemplateVersionHealthStatus(
      serviceConfiguration = serviceConfig2,
      status = HealthStatusType.HEALTHY,
    )

    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationIds(any()))
      .thenReturn(listOf(t1, t2))

    val inputIds = listOf(
      serviceConfig1.id,
      serviceConfig2.id,
      UUID.randomUUID(),
    )

    val actual = updateTemplateVersionHealthService.getTemplateHealthStatusByServiceConfigurationIds(inputIds)
    assertThat(actual).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        serviceConfig1.id to t1,
        serviceConfig2.id to t2,
      ),
    )

    verify(templateVersionHealthStatusRepository, times(1))
      .findByServiceConfigurationIds(inputIds)
  }
}
