package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.PrisonDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.PrisonDetailsRepository

class UpdatePrisonNameDataTest {

  private val prisonRepository: PrisonDetailsRepository = mock()
  private val prisonRegisterClient: PrisonRegisterClient = mock()

  private val updatePrisonNameDataService = UpdatePrisonNameDataService(prisonRepository, prisonRegisterClient)

  private val prisonDetailsList = listOf(
    PrisonDetails(prisonId = "AKI", prisonName = "Acklington (HMP)"),
    PrisonDetails(prisonId = "ALI", prisonName = "Albany (HMP)"),
    PrisonDetails(prisonId = "ANI", prisonName = "Aldington (HMP)"),
    PrisonDetails(prisonId = "MDI", prisonName = "Moorland (HMP & YOI)"),
  )

  @Test
  fun `should update prison name data`() {
    whenever(prisonRegisterClient.getPrisonDetails()).thenReturn(prisonDetailsList)

    updatePrisonNameDataService.updatePrisonIdData()

    verify(prisonRegisterClient, times(1)).getPrisonDetails()
    verifyNoMoreInteractions(prisonRegisterClient)

    verify(prisonRepository, times(4)).save(any())
  }
}
