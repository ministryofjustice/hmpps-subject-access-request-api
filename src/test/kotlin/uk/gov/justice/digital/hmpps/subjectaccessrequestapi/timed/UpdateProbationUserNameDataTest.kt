package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.UserDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.UserDetailsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.UserDetailsRepository

class UpdateProbationUserNameDataTest {

  private val userDetailsRepository: UserDetailsRepository = mock()
  private val userDetailsClient: UserDetailsClient = mock()

  private val updateProbationUserDataService = UpdateProbationUserNameDataService(userDetailsRepository, userDetailsClient)

  private val userDetailsList = listOf(
    UserDetails(username = "AA46243", lastName = "SMITH"),
    UserDetails(username = "ALI241", lastName = "JONES"),
    UserDetails(username = "DB128Z", lastName = "ALI"),
    UserDetails(username = "2GRMDI", lastName = "LEE"),
  )

  @Test
  fun `should update User name data`() {
    whenever(userDetailsClient.getProbationUserDetails()).thenReturn(userDetailsList)

    updateProbationUserDataService.updateProbationUserData()

    verify(userDetailsClient, times(1)).getProbationUserDetails()
    verifyNoMoreInteractions(userDetailsClient)

    verify(userDetailsRepository, times(4)).save(any())
  }

  @Test
  fun `should update User name data will not save entries with empty lastname`() {
    val userDetailsList = listOf(
      UserDetails(username = "AA46243", lastName = "SMITH"),
      UserDetails(username = "ALI241", lastName = ""),
    )
    whenever(userDetailsClient.getProbationUserDetails()).thenReturn(userDetailsList)

    updateProbationUserDataService.updateProbationUserData()

    verify(userDetailsClient, times(1)).getProbationUserDetails()
    verifyNoMoreInteractions(userDetailsClient)

    verify(userDetailsRepository, times(1)).save(any())
  }
}
