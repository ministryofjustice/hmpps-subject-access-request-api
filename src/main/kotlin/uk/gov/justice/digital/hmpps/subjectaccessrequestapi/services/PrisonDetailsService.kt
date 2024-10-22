package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.PrisonDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.PrisonDetailsRepository

@Service
class PrisonDetailsService(
  private val prisonDetailsRepository: PrisonDetailsRepository,
) {

  fun getPrisonName(prisonId: String): PrisonDetail = prisonDetailsRepository.findByPrisonId(prisonId)
}
