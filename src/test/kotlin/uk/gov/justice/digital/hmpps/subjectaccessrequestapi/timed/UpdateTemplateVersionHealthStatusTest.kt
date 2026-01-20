package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionHealthStatusException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.TemplateVersionHealthService
import java.util.UUID

class UpdateTemplateVersionHealthStatusTest {

  companion object {
    private val serviceConfigurationOne = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "test1",
      label = "Test One",
      url = "url",
      order = 123,
      enabled = true,
      templateMigrated = true,
      category = ServiceCategory.PRISON,
    )
    private val serviceConfigurationTwo = ServiceConfiguration(
      id = UUID.randomUUID(),
      serviceName = "test2",
      label = "Test Two",
      url = "url",
      order = 456,
      enabled = true,
      templateMigrated = true,
      category = ServiceCategory.PRISON,
    )
    private val serviceConfigurationThree = ServiceConfiguration(
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

  private val serviceConfigurationRepository: ServiceConfigurationRepository = mock()
  private val templateVersionHealthService: TemplateVersionHealthService = mock()

  private val updateTemplateVersionHealthService = UpdateTemplateVersionHealthService(
    serviceConfigurationRepository,
    templateVersionHealthService,
  )

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(templateVersionHealthService)
  }

  @Test
  fun `should not perform any updates when no services found`() {
    whenever(serviceConfigurationRepository.findAllByEnabledAndTemplateMigrated()).thenReturn(emptyList())

    updateTemplateVersionHealthService.updateTemplateVersionHealthData()

    verifyNoInteractions(templateVersionHealthService)
  }

  @Test
  fun `should update status for multiple services`() {
    whenever(serviceConfigurationRepository.findAllByEnabledAndTemplateMigrated()).thenReturn(
      listOf(
        serviceConfigurationOne,
        serviceConfigurationTwo,
        serviceConfigurationThree,
      ),
    )

    updateTemplateVersionHealthService.updateTemplateVersionHealthData()

    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationOne)
    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationTwo)
    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationThree)
  }

  @Test
  fun `should return failure status when error for one service and continue with others`() {
    whenever(serviceConfigurationRepository.findAllByEnabledAndTemplateMigrated()).thenReturn(
      listOf(
        serviceConfigurationOne,
        serviceConfigurationTwo,
        serviceConfigurationThree,
      ),
    )
    whenever(templateVersionHealthService.updateTemplateVersionHealthData(eq(serviceConfigurationTwo))).thenThrow(
      RuntimeException("Test exception"),
    )

    val exception =
      assertThrows<TemplateVersionHealthStatusException> { updateTemplateVersionHealthService.updateTemplateVersionHealthData() }

    assertThat(exception).hasMessage("At least one template version health status could not be updated")
    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationOne)
    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationTwo)
    verify(templateVersionHealthService).updateTemplateVersionHealthData(serviceConfigurationThree)
  }
}
