package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.PrisonDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.PrisonDetailsRepository

/**
 * Refresh prison cache daily - 24hours = 86400000 milliseconds
 */
@Component
class UpdatePrisonNameData(private val service: UpdatePrisonNameDataService) {

  @Scheduled(
    fixedDelayString = "\${application.prison-refresh.frequency}",
    initialDelayString = "\${random.int[60000,\${application.prison-refresh.frequency}]}",
  )
  fun updatePrisonCache() {
    try {
      service.updatePrisonData()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during prison cache update", e.javaClass.simpleName, e)
      Sentry.captureException(e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class UpdatePrisonNameDataService(
  private val prisonRepository: PrisonDetailsRepository,
  private val prisonRegisterClient: PrisonRegisterClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updatePrisonData() {
    log.info("updating prison details in database")

    prisonRegisterClient.getPrisonDetails().forEach {
      prisonRepository.save(PrisonDetail(prisonId = it.prisonId, prisonName = it.prisonName))
    }

    log.info("prison details updated in database")
  }
}
