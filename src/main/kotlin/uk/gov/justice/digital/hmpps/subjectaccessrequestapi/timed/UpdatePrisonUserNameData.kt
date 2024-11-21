package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.UserDetailsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.UserDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.UserDetailsRepository

/**
 * Refresh prison cache daily - 24hours = 86400000 milliseconds
 */
@Component
class UpdatePrisonUserNameData(private val service: UpdatePrisonUserNameDataService) {

  @Scheduled(
    fixedDelayString = "\${application.user-details-refresh.frequency}",
    initialDelayString = "\${random.int[60000,\${application.user-details-refresh.frequency}]}",
  )
  fun updateUserCache() {
    try {
      service.updatePrisonUserData()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during User cache update", e.javaClass.simpleName, e)
      Sentry.captureException(e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class UpdatePrisonUserNameDataService(
  private val userDetailsRepository: UserDetailsRepository,
  private val userDetailsClient: UserDetailsClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updatePrisonUserData() {
    log.info("updating prison user details in database")

    userDetailsClient.getNomisUserDetails().forEach {
      if (it.lastName.isNotBlank()) {
        userDetailsRepository.save(UserDetail(username = it.username, lastName = it.lastName))
      }
    }

    log.info("prison users details obtained, updated in database")
  }
}
