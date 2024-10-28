package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.PrisonDetail

@DataJpaTest
class PrisonDetailsRepositoryTest {

  @Autowired
  lateinit var prisonDetailsRepository: PrisonDetailsRepository

  @BeforeEach
  fun setup() {
    prisonDetailsRepository.deleteAll()
    prisonDetailsRepository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))
  }

  @Test
  fun testFindByPrisonId() {
    val prisonDetails = prisonDetailsRepository.findById("AKI").orElseThrow()

    assertThat(prisonDetails).isNotNull
    assertThat(prisonDetails)
      .extracting("prisonId").isEqualTo("AKI")

    assertThat(prisonDetails)
      .extracting("prisonName").isEqualTo("Acklington (HMP)")
  }

  @Test
  fun savingPrisonDetails() {
    val before = prisonDetailsRepository.findAll()

    prisonDetailsRepository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))

    val after = prisonDetailsRepository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(3)
    assertThat(before).isEqualTo(after)
  }

  @Test
  fun savingPrisonDetailsNewPrison() {
    val before = prisonDetailsRepository.findAll()

    prisonDetailsRepository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))
    prisonDetailsRepository.save(PrisonDetail(prisonId = "MDI", prisonName = "Moorland (HMP & YOI)"))

    val after = prisonDetailsRepository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(4)
  }
}
