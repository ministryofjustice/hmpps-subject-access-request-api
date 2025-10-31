package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionServiceConfigurationNotFoundException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionTemplateBodyEmptyException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class TemplateVersionServiceTest {

  private val templateVersionRepository: TemplateVersionRepository = mock()
  private val serviceConfigurationRepository: ServiceConfigurationRepository = mock()

  private val templateBody = "Once upon a midnight dreary, while I pondered, weak and weary"
  private val expectedHashValue = "18f918f8e6ebefe1e3795f4d82fdcce58fb2db0193a3c46719459438abc4dfac"

  private val templateVersionService = TemplateVersionService(
    templateVersionRepository,
    serviceConfigurationRepository,
  )

  private val serviceConfig: ServiceConfiguration = mock()

  private val templateV1 = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig,
    status = TemplateVersionStatus.PUBLISHED,
    version = 1,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v1-hash",
  )

  private val templateV2 = TemplateVersion(
    id = UUID.randomUUID(),
    serviceConfiguration = serviceConfig,
    status = TemplateVersionStatus.PENDING,
    version = 2,
    createdAt = LocalDateTime.now(),
    fileHash = "template-v2-hash",
  )

  @Nested
  inner class GetVersions {

    @Test
    fun `should return expected versions`() {
      val serviceConfigId = UUID.randomUUID()
      whenever(templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfigId))
        .thenReturn(listOf(templateV1, templateV2))

      assertThat(templateVersionService.getVersions(serviceConfigId)).containsExactly(templateV1, templateV2)

      verify(templateVersionRepository, times(1))
        .findByServiceConfigurationIdOrderByVersionDesc(serviceConfigId)
    }
  }

  @Nested
  inner class GetHashValue {

    @Test
    fun `should return expected hash value`() {
      assertThat(templateVersionService.getHashValue(templateBody.toByteArray())).isEqualTo(expectedHashValue)
    }
  }

  @Nested
  inner class GetNextVersionForService {

    @Test
    fun `should return 1 when there is no existing version`() {
      whenever(templateVersionRepository.findLatestByServiceConfigurationId(any())).thenReturn(null)
      assertThat(templateVersionService.getNextVersionForService(UUID.randomUUID())).isEqualTo(1)
    }

    @Test
    fun `should return expected value when there is an existing version`() {
      whenever(templateVersionRepository.findLatestByServiceConfigurationId(any()))
        .thenReturn(TemplateVersion(version = 1))

      assertThat(templateVersionService.getNextVersionForService(UUID.randomUUID())).isEqualTo(2)
    }
  }

  @Nested
  inner class DeletePendingTemplateVersionsByServiceConfigurationId {

    @Test
    fun `should delete the expected template versions`() {
      val id = UUID.randomUUID()
      templateVersionService.deletePendingTemplateVersionsByServiceConfigurationId(id)

      val idCaptor = argumentCaptor<UUID>()
      val statusCaptor = argumentCaptor<TemplateVersionStatus>()

      verify(templateVersionRepository, times(1)).deleteByServiceConfigurationIdAndStatus(
        idCaptor.capture(),
        statusCaptor.capture(),
      )
      assertThat(idCaptor.allValues).containsExactly(id)
      assertThat(statusCaptor.allValues).containsExactly(TemplateVersionStatus.PENDING)
    }
  }

  @Nested
  inner class SaveNewTemplateVersion {

    @Test
    fun `should throw exception when service configuration ID not found`() {
      val serviceConfigId = UUID.randomUUID()
      whenever(serviceConfigurationRepository.findById(serviceConfigId)).thenReturn(Optional.empty())

      val actual = assertThrows<TemplateVersionServiceConfigurationNotFoundException> {
        templateVersionService.saveNewTemplateVersion(serviceConfigId, "")
      }

      assertThat(actual.message)
        .isEqualTo("create template version error service configuration id: $serviceConfigId not found")

      verify(serviceConfigurationRepository, times(1)).findById(serviceConfigId)
      verifyNoInteractions(templateVersionRepository)
    }

    @Test
    fun `should throw exception when template body is empty`() {
      val serviceConfigId = UUID.randomUUID()
      whenever(serviceConfigurationRepository.findById(serviceConfigId)).thenReturn(Optional.of(serviceConfig))

      val actual = assertThrows<TemplateVersionTemplateBodyEmptyException> {
        templateVersionService.saveNewTemplateVersion(serviceConfigId, "")
      }

      assertThat(actual.message)
        .isEqualTo("create template version error for service: $serviceConfigId: template body was empty")

      verify(serviceConfigurationRepository, times(1)).findById(serviceConfigId)
      verifyNoInteractions(templateVersionRepository)
    }

    @Test
    fun `should create version 1 when no previous version exists`() {
      val serviceConfigId = UUID.randomUUID()
      whenever(serviceConfigurationRepository.findById(serviceConfigId)).thenReturn(Optional.of(serviceConfig))
      whenever(serviceConfig.id).thenReturn(serviceConfigId)
      whenever(templateVersionRepository.findLatestByServiceConfigurationId(serviceConfigId)).thenReturn(null)
      whenever(templateVersionRepository.save(any())).thenReturn(TemplateVersion())

      val templateVersionCaptor = argumentCaptor<TemplateVersion>()

      templateVersionService.saveNewTemplateVersion(serviceConfigId, templateBody)

      verify(serviceConfigurationRepository, times(1)).findById(serviceConfigId)
      verify(templateVersionRepository, times(1)).deleteByServiceConfigurationIdAndStatus(
        serviceConfigId,
        TemplateVersionStatus.PENDING,
      )
      verify(templateVersionRepository, times(1)).save(templateVersionCaptor.capture())

      assertThat(templateVersionCaptor.allValues).hasSize(1)
      val actual = templateVersionCaptor.allValues.first()
      assertThat(actual.id).isNotNull()
      assertThat(actual.serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(actual.status).isEqualTo(TemplateVersionStatus.PENDING)
      assertThat(actual.version).isEqualTo(1)
      assertThat(actual.fileHash).isEqualTo(expectedHashValue)
    }

    @Test
    fun `should create the expected next version when a previous version already exists`() {
      val serviceConfigId = UUID.randomUUID()
      whenever(serviceConfigurationRepository.findById(serviceConfigId)).thenReturn(Optional.of(serviceConfig))
      whenever(serviceConfig.id).thenReturn(serviceConfigId)
      whenever(templateVersionRepository.findLatestByServiceConfigurationId(serviceConfigId))
        .thenReturn(TemplateVersion(version = 1))
      whenever(templateVersionRepository.save(any())).thenReturn(TemplateVersion())

      val templateVersionCaptor = argumentCaptor<TemplateVersion>()

      templateVersionService.saveNewTemplateVersion(serviceConfigId, templateBody)

      verify(serviceConfigurationRepository, times(1)).findById(serviceConfigId)
      verify(templateVersionRepository, times(1)).deleteByServiceConfigurationIdAndStatus(
        serviceConfigId,
        TemplateVersionStatus.PENDING,
      )
      verify(templateVersionRepository, times(1)).save(templateVersionCaptor.capture())

      assertThat(templateVersionCaptor.allValues).hasSize(1)
      val actual = templateVersionCaptor.allValues.first()
      assertThat(actual.id).isNotNull()
      assertThat(actual.serviceConfiguration).isEqualTo(serviceConfig)
      assertThat(actual.status).isEqualTo(TemplateVersionStatus.PENDING)
      assertThat(actual.version).isEqualTo(2)
      assertThat(actual.fileHash).isEqualTo(expectedHashValue)
    }
  }
}
