package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.RendererServiceFailureType
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant

interface RendererServiceCallFailedAlertLimiter {
  fun shouldSend(
    serviceName: String,
    failureType: RendererServiceFailureType,
  ): Boolean
}

@Service
class ShedLockRendererServiceCallFailedAlertLimiter(
  private val lockProvider: LockProvider,
  private val clock: Clock,
  @param:Value("\${slack.bot.notification-cooldown.service-call-failed}") private val notificationCooldown: Duration,
) : RendererServiceCallFailedAlertLimiter {

  companion object {
    private const val LOCK_NAME_PREFIX = "renderer-failed-"
    private const val SHEDLOCK_NAME_MAX_LENGTH = 64
  }

  override fun shouldSend(
    serviceName: String,
    failureType: RendererServiceFailureType,
  ): Boolean {
    if (notificationCooldown.isZero || notificationCooldown.isNegative) {
      return true
    }

    val lock = lockProvider.lock(
      LockConfiguration(
        Instant.now(clock),
        lockNameFor(serviceName, failureType),
        notificationCooldown,
        notificationCooldown,
      ),
    ).orElse(null) ?: return false

    lock.unlock()
    return true
  }

  private fun lockNameFor(
    serviceName: String,
    failureType: RendererServiceFailureType,
  ): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest("$serviceName:${failureType.name}".toByteArray())
      .joinToString("") { "%02x".format(it) }

    return LOCK_NAME_PREFIX + digest.take(SHEDLOCK_NAME_MAX_LENGTH - LOCK_NAME_PREFIX.length)
  }
}
