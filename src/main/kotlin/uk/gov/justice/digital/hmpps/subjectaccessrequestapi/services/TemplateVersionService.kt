package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionServiceConfigurationNotFoundException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.exceptions.TemplateVersionTemplateBodyEmptyException
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersion
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionRepository
import java.security.MessageDigest
import java.util.UUID

@Service
class TemplateVersionService(
  private val templateVersionRepository: TemplateVersionRepository,
  private val serviceConfigurationRepository: ServiceConfigurationRepository,
  private val notificationService: NotificationService,
) {

  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun getVersions(
    serviceConfigurationId: UUID,
  ): List<TemplateVersion>? = templateVersionRepository.findByServiceConfigurationIdOrderByVersionDesc(
    serviceConfigurationId,
  )

  fun getHashValue(bytes: ByteArray): String {
    val hashBytes = MessageDigest.getInstance("SHA-256").digest(bytes)
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  @Transactional
  fun saveNewTemplateVersion(serviceId: UUID, template: String): TemplateVersion {
    val serviceConfiguration = getServiceConfigurationOrError(serviceId)

    val templateHash = template.takeIf { it.isNotEmpty() }
      ?.let { getHashValue(it.toByteArray()) }
      ?: throw TemplateVersionTemplateBodyEmptyException(serviceId)

    deletePendingTemplateVersionsByServiceConfigurationId(serviceConfiguration.id)
    val nextVersion = getNextVersionForService(serviceConfiguration.id)

    LOG.info(
      "saving new template version for {}, nextVersion: {}, status: {}",
      serviceConfiguration.serviceName,
      nextVersion,
      TemplateVersionStatus.PENDING,
    )

    val newTemplateVersion = templateVersionRepository.save(
      TemplateVersion(
        id = UUID.randomUUID(),
        serviceConfiguration = serviceConfiguration,
        status = TemplateVersionStatus.PENDING,
        version = nextVersion,
        fileHash = templateHash,
      ),
    )
    notificationService.sendNewTemplateVersionNotification(newTemplateVersion)
    return newTemplateVersion
  }

  @Transactional
  fun getNextVersionForService(
    serviceId: UUID,
  ): Int = templateVersionRepository.findLatestByServiceConfigurationId(serviceId)?.let { it.version + 1 } ?: 1

  @Transactional
  fun deletePendingTemplateVersionsByServiceConfigurationId(
    id: UUID,
  ) = templateVersionRepository.deleteByServiceConfigurationIdAndStatus(id, TemplateVersionStatus.PENDING)

  private fun getServiceConfigurationOrError(id: UUID) = serviceConfigurationRepository.findByIdOrNull(id)
    ?: throw TemplateVersionServiceConfigurationNotFoundException(id)

  fun isTemplateHashValid (serviceConfigurationId: UUID, templateHash: String): Boolean {
    val versions = getVersions(serviceConfigurationId).orEmpty()
    if (versions.isEmpty()) return false
    val latest = versions.first()
    if (latest.status == TemplateVersionStatus.PUBLISHED) {
      return latest.fileHash == templateHash
    }
    return latest.fileHash == templateHash || versions.getOrNull(1)?.fileHash == templateHash
  }
}
