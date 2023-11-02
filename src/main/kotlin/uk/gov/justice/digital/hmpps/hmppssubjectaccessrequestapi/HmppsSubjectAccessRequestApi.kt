package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsSubjectAccessRequestApi

fun main(args: Array<String>) {
  runApplication<HmppsSubjectAccessRequestApi>(*args)
}
