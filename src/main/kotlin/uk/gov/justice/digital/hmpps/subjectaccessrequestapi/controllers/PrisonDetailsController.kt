package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.controllers

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services.PrisonDetailsService

@RestController
class PrisonDetailsController(
  private val prisonDetailsService: PrisonDetailsService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/prison/{id}")
  fun getPrisons(@PathVariable("id") id: String) = prisonDetailsService.getPrisonName(id).prisonName
}
