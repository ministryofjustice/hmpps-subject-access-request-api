package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.UserDetail

@DataJpaTest
class NameDetailsRepositoryTest {

  @Autowired
  lateinit var userDetailsRepository: UserDetailsRepository

  @BeforeEach
  fun setup() {
    userDetailsRepository.deleteAll()
    userDetailsRepository.save(UserDetail(username = "AA46243", lastName = "SMITH"))
    userDetailsRepository.save(UserDetail(username = "ALI241", lastName = "JONES"))
    userDetailsRepository.save(UserDetail(username = "DB128Z", lastName = "ALI"))
  }

  @Test
  fun testFindByUsername() {
    val userDetails = userDetailsRepository.findById("ALI241").orElseThrow()

    assertThat(userDetails).isNotNull
    assertThat(userDetails)
      .extracting("username").isEqualTo("ALI241")

    assertThat(userDetails)
      .extracting("lastName").isEqualTo("JONES")
  }

  @Test
  fun savingUserDetails() {
    val before = userDetailsRepository.findAll()

    userDetailsRepository.save(UserDetail(username = "AA46243", lastName = "SMITH"))
    userDetailsRepository.save(UserDetail(username = "ALI241", lastName = "JONES"))
    userDetailsRepository.save(UserDetail(username = "DB128Z", lastName = "ALI"))

    val after = userDetailsRepository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(3)
    assertThat(before).isEqualTo(after)
  }

  @Test
  fun savingUserDetailsNewUser() {
    val before = userDetailsRepository.findAll()

    userDetailsRepository.save(UserDetail(username = "AA46243", lastName = "SMITH"))
    userDetailsRepository.save(UserDetail(username = "ALI241", lastName = "JONES"))
    userDetailsRepository.save(UserDetail(username = "DB128Z", lastName = "ALI"))
    userDetailsRepository.save(UserDetail(username = "2GRMDI", lastName = "LEE"))

    val after = userDetailsRepository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(4)
  }
}
