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

class UpdateUserNameDataTest {

  private val userDetailsRepository: UserDetailsRepository = mock()
  private val userDetailsClient: UserDetailsClient = mock()

  private val updateUserNameDataService = UpdateUserNameDataService(userDetailsRepository, userDetailsClient)

  private val userDetailsList = listOf(
    UserDetails(username = "AA46243", lastName = "SMITH"),
    UserDetails(username = "ALI241", lastName = "JONES"),
    UserDetails(username = "DB128Z", lastName = "ALI"),
    UserDetails(username = "2GRMDI", lastName = "LEE"),
  )

  @Test
  fun `should update User name data`() {
    whenever(userDetailsClient.getNomisUserDetails()).thenReturn(userDetailsList)

    updateUserNameDataService.updateUserData()

    verify(userDetailsClient, times(1)).getNomisUserDetails()
    verifyNoMoreInteractions(userDetailsClient)

    verify(userDetailsRepository, times(4)).save(any())
  }
}
