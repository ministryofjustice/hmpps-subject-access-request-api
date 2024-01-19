package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import java.time.Duration
class WorkerService {
  fun runThread() {
    Thread {
      do {
        val response = getDatabaseUpdates()
        Thread.sleep(Duration.ofSeconds(30))
      } while (response == "")
    }.start()
  }

  private fun getDatabaseUpdates(): String {
    val random = (0..10).random()
    if (random % 2 == 0 ) {
      return random.toString()
    }
    return ""
  }
}
