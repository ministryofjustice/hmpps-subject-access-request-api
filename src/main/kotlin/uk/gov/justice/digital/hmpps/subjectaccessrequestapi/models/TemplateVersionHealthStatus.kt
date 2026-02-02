package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "TEMPLATE_VERSION_HEALTH_STATUS")
data class TemplateVersionHealthStatus(
  @Id
  val id: UUID = UUID.randomUUID(),

  @OneToOne
  @JoinColumn(name = "service_configuration_id")
  val serviceConfiguration: ServiceConfiguration,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  val status: HealthStatusType? = null,

  @Column(name = "last_modified", nullable = false)
  val lastModified: Instant = Instant.now(),

  @Column(name = "last_notified", nullable = true)
  var lastNotified: Instant? = null,
)

enum class HealthStatusType {
  HEALTHY,
  UNHEALTHY,
  NOT_MIGRATED,
}
