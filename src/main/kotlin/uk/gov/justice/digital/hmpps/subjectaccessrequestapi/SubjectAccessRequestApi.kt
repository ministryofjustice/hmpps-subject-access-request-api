package uk.gov.justice.digital.hmpps.subjectaccessrequestapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication()
class SubjectAccessRequestApi

fun main(args: Array<String>) {
  runApplication<SubjectAccessRequestApi>(*args)
}

@Configuration
@EnableScheduling
class SchedulingConfiguration
