package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
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

    private val serviceConfiguration = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "test1",
      label = "Test One",
      url = "url",
      order = 123,
      enabled = true,
      templateMigrated = true,
      category = ServiceCategory.PRISON,
    )
  }

  private val templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository = mock()
  private val templateVersionService: TemplateVersionService = mock()
  private val dynamicServicesClient: DynamicServicesClient = mock()
  private val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

  private val updateTemplateVersionHealthService = TemplateVersionHealthService(
    templateVersionHealthStatusRepository,
    templateVersionService,
    dynamicServicesClient,
    clock,
    10L,
    10L,
  )

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(templateVersionHealthStatusRepository)
  }

  @Test
  fun `should not update status when template not found`() {
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfiguration)).thenReturn(null)

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfiguration)

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
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfiguration)).thenReturn(TEMPLATE_ONE)
    whenever(
      templateVersionService.isTemplateHashValid(
        serviceConfiguration.id,
        "0a1bf7a8b5b414b50e3b9a0c746e1b493dadddaf74ba3861ff4f663ea65938d2",
      ),
    ).thenReturn(fileHashValid)
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfiguration.id))
      .thenReturn(null)

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfiguration)

    verify(templateVersionHealthStatusRepository).findByServiceConfigurationId(serviceConfiguration.id)
    verify(templateVersionHealthStatusRepository).save(
      argThat { templateVersionHealth ->
        templateVersionHealth.serviceConfiguration == serviceConfiguration &&
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
    whenever(dynamicServicesClient.getServiceTemplate(serviceConfiguration)).thenReturn(TEMPLATE_ONE)
    whenever(
      templateVersionService.isTemplateHashValid(
        serviceConfiguration.id,
        "0a1bf7a8b5b414b50e3b9a0c746e1b493dadddaf74ba3861ff4f663ea65938d2",
      ),
    ).thenReturn(fileHashValid)
    whenever(templateVersionHealthStatusRepository.findByServiceConfigurationId(serviceConfiguration.id))
      .thenReturn(TemplateVersionHealthStatus())

    updateTemplateVersionHealthService.updateTemplateVersionHealthData(serviceConfiguration)

    verify(templateVersionHealthStatusRepository).findByServiceConfigurationId(serviceConfiguration.id)
    verify(templateVersionHealthStatusRepository).updateStatusWhenChanged(
      serviceConfiguration.id,
      expectedHealthStatus,
      NOW,
    )
  }
}
