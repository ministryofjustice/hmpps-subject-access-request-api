package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.alerts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.HealthStatusType
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceCategory
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.ServiceConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.TemplateVersionHealthStatus
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.ServiceConfigurationRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.TemplateVersionHealthStatusRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.SlackNotificationService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TemplateVersionHealthAlertTest : IntegrationTestBase() {

  @Autowired
  private lateinit var templateVersionHealthAlert: TemplateVersionHealthAlert

  @Autowired
  private lateinit var templateVersionHealthStatusRepository: TemplateVersionHealthStatusRepository

  @Autowired
  private lateinit var serviceConfigurationRepository: ServiceConfigurationRepository

  @MockitoBean
  private lateinit var slackNotificationService: SlackNotificationService

  private val serviceConfig1 = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test-001",
    label = "Service-1",
    url = "http://localhost:8080",
    order = 900,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val serviceConfig2 = ServiceConfiguration(
    id = UUID.randomUUID(),
    serviceName = "test-002",
    label = "Service-2",
    url = "http://localhost:8080",
    order = 901,
    enabled = true,
    templateMigrated = true,
    category = ServiceCategory.PRISON,
  )

  private val now = Instant.now()

  @BeforeEach
  fun setup() {
    templateVersionHealthStatusRepository.deleteAll()
    serviceConfigurationRepository.saveAllAndFlush(listOf(serviceConfig1, serviceConfig2))
  }

  @AfterEach
  fun teardown() {
    templateVersionHealthStatusRepository.deleteAll()
    serviceConfigurationRepository.deleteAllById(listOf(serviceConfig1.id, serviceConfig2.id))
  }

  @Test
  fun `should not raise alert when no template version health statuses exist`() {
    templateVersionHealthAlert.raiseAlerts()

    verifyNoInteractions(slackNotificationService)
  }

  @Test
  fun `should not raise alert when no template version health statuses as unhealthy`() {
    templateVersionHealthStatusRepository.saveAllAndFlush(
      listOf(
        TemplateVersionHealthStatus(
          status = HealthStatusType.HEALTHY,
          serviceConfiguration = serviceConfig1,
          lastModified = now.minusMinutes(40),
          lastNotified = null,
        ),
        TemplateVersionHealthStatus(
          status = HealthStatusType.HEALTHY,
          serviceConfiguration = serviceConfig2,
          lastModified = now.minusMinutes(5),
          lastNotified = null,
        ),
      ),
    )

    templateVersionHealthAlert.raiseAlerts()

    verifyNoInteractions(slackNotificationService)
  }

  @Test
  fun `should not raise alert when unhealthy threshold has not been met`() {
    // Unhealthy threshold is 10min (see application-test.yml) this one has only been unhealthy for 8mins.
    templateVersionHealthStatusRepository.saveAndFlush(
      TemplateVersionHealthStatus(
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now.minusMinutes(8),
        lastNotified = null,
      ),
    )

    templateVersionHealthAlert.raiseAlerts()

    verifyNoInteractions(slackNotificationService)
  }

  @Test
  fun `should not raise alert when unhealthy threshold is met but last notified threshold is not`() {
    // last notified threshold is 120min (see application-test.yml)
    templateVersionHealthStatusRepository.saveAndFlush(
      TemplateVersionHealthStatus(
        status = HealthStatusType.UNHEALTHY,
        serviceConfiguration = serviceConfig1,
        lastModified = now.minusMinutes(20),
        lastNotified = now.minusMinutes(100),
      ),
    )

    templateVersionHealthAlert.raiseAlerts()

    verifyNoInteractions(slackNotificationService)
  }

  @Test
  fun `should raise alert when unhealthy threshold and last notified threshold are met`() {
    // last notified threshold is 120min (see application-test.yml)
    val lastModified = now.minusMinutes(20)
    val lastNotified = now.minusMinutes(123)

    val unhealthyTemplateVersion = TemplateVersionHealthStatus(
      status = HealthStatusType.UNHEALTHY,
      serviceConfiguration = serviceConfig1,
      lastModified = lastModified,
      lastNotified = lastNotified,
    )

    templateVersionHealthStatusRepository.saveAndFlush(unhealthyTemplateVersion)

    templateVersionHealthAlert.raiseAlerts()

    verify(slackNotificationService, times(1))
      .sendTemplateHealthAlert(
        argThat { input ->
          assertThat(input).hasSize(1)
          assertThat(input[0].status).isEqualTo(HealthStatusType.UNHEALTHY)
          assertThat(input[0].serviceConfiguration).isEqualTo(serviceConfig1)

          assertThat(input[0].lastModified.truncatedTo(ChronoUnit.MILLIS))
            .isEqualTo(lastModified.truncatedTo(ChronoUnit.MILLIS))

          assertThat(input[0].lastNotified!!.truncatedTo(ChronoUnit.MILLIS))
            .isEqualTo(lastNotified.truncatedTo(ChronoUnit.MILLIS))

          true
        },
      )
  }

  private fun Instant.minusMinutes(
    amount: Long,
  ): Instant = this.minus(amount, ChronoUnit.MINUTES)
}
