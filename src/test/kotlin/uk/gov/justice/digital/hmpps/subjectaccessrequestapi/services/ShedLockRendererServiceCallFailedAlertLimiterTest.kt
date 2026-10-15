package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers.entity.RendererServiceFailureType
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class ShedLockRendererServiceCallFailedAlertLimiterTest {

  private val lockProvider: LockProvider = mock()
  private val simpleLock: SimpleLock = mock()
  private val clock: Clock = Clock.fixed(Instant.parse("2026-01-22T14:30:00Z"), ZoneOffset.UTC)

  @Test
  fun `should allow send when lock is acquired`() {
    whenever(lockProvider.lock(any())).thenReturn(Optional.of(simpleLock))

    val limiter = ShedLockRendererServiceCallFailedAlertLimiter(
      lockProvider = lockProvider,
      clock = clock,
      notificationCooldown = Duration.ofMinutes(15),
    )

    val shouldSend = limiter.shouldSend("service-1", RendererServiceFailureType.SAR_DATA)

    assertThat(shouldSend).isTrue()
    verify(simpleLock).unlock()
  }

  @Test
  fun `should suppress send when lock is not acquired`() {
    whenever(lockProvider.lock(any())).thenReturn(Optional.empty())

    val limiter = ShedLockRendererServiceCallFailedAlertLimiter(
      lockProvider = lockProvider,
      clock = clock,
      notificationCooldown = Duration.ofMinutes(15),
    )

    val shouldSend = limiter.shouldSend("service-1", RendererServiceFailureType.SAR_DATA)

    assertThat(shouldSend).isFalse()
    verify(simpleLock, never()).unlock()
  }

  @Test
  fun `should allow send without lock when cooldown is disabled`() {
    val limiter = ShedLockRendererServiceCallFailedAlertLimiter(
      lockProvider = lockProvider,
      clock = clock,
      notificationCooldown = Duration.ZERO,
    )

    val shouldSend = limiter.shouldSend("service-1", RendererServiceFailureType.SAR_DATA)

    assertThat(shouldSend).isTrue()
    verifyNoInteractions(lockProvider)
  }
}
