package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "SERVICE_CONFIGURATION")
data class ServiceConfiguration(

  @Id
  val id: UUID = UUID.randomUUID(),

  @Column(name = "service_name", nullable = false)
  var serviceName: String,

  @Column(name = "label", nullable = false)
  var label: String,

  @Column(name = "url", nullable = false)
  var url: String,

  @Column(name = "enabled", nullable = false)
  var enabled: Boolean,

  @Column(name = "template_migrated", nullable = false)
  var templateMigrated: Boolean,

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  var category: ServiceCategory,

  @Column(name = "suspended", nullable = false)
  var suspended: Boolean = false,

  @Column(name = "suspended_at", nullable = true)
  var suspendedAt: Instant? = null,
)

enum class ServiceCategory {
  PRISON,
  PROBATION,
  ;

  companion object {
    @JvmStatic
    fun valueOfOrNull(value: String): ServiceCategory? = try {
      valueOf(value)
    } catch (e: Exception) {
      return null
    }
  }
}
