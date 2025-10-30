package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

@Service
class TemplateVersionService(
  private val templateVersionRepository: TemplateVersionRepository,
  private val serviceConfigurationRepository: ServiceConfigurationRepository,
) {

  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun getVersions(
    serviceConfigurationId: UUID,
  ): List<TemplateVersion>? =
    templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(serviceConfigurationId)

  fun getHashValue(bytes: ByteArray): String {
    val hashBytes = MessageDigest.getInstance("SHA-256").digest(bytes)
    return  hashBytes.joinToString("") { "%02x".format(it) }
  }

  @Transactional
  fun newTemplateVersion(serviceId: UUID, template: String): TemplateVersion {
    val serviceConfiguration = getServiceConfigurationOrError(serviceId)

    return template.takeIf { it.isNotEmpty() }?.let {
      deletePendingTemplateVersionsByServiceConfigurationId(serviceConfiguration.id)
      val nextVersion = getNextVersionForService(serviceConfiguration.id)

      LOG.info(
        "saving new template version for {}, nextVersion: {}, status: {}",
        serviceConfiguration.serviceName,
        nextVersion,
        TemplateStatus.PENDING,
      )

      templateVersionRepository.save(
        TemplateVersion(
          id = UUID.randomUUID(),
          serviceConfiguration = serviceConfiguration,
          status = TemplateStatus.PENDING,
          version = nextVersion,
          createdAt = LocalDateTime.now(),
          fileHash = getHashValue(template.toByteArray()),
        ),
      )
    } ?: throw RuntimeException("Template body was empty")
  }

  @Transactional
  fun getNextVersionForService(
    serviceId: UUID,
  ): Int = templateVersionRepository.findLatestByServiceConfigurationId(serviceId)?.let { it.version + 1 } ?: 1


  @Transactional
  fun deletePendingTemplateVersionsByServiceConfigurationId(
    id: UUID
  ) = templateVersionRepository.deleteByServiceConfigurationIdAndStatus(id, TemplateStatus.PENDING)

  fun getServiceConfigurationOrError(id: UUID) = serviceConfigurationRepository.findByIdOrNull(id)
    ?: throw RuntimeException("service $id not found") // todo
}