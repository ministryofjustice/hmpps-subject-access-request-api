package uk.gov.justice.digital.hmpps.subjectaccessrequestapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class SubjectAccessRequestApi

fun main(args: Array<String>) {
  runApplication<SubjectAccessRequestApi>(*args)
}
