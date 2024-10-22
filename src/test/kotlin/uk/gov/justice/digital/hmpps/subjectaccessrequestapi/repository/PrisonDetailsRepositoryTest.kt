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
  lateinit var repository: PrisonDetailsRepository

  @BeforeEach
  fun setup() {
    repository.deleteAll()
    repository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    repository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    repository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))
  }

  @Test
  fun testFindByPrisonId() {
    val prisonDetails = repository.findById("AKI").orElseThrow()

    assertThat(prisonDetails).isNotNull
    assertThat(prisonDetails)
      .extracting("prisonId").isEqualTo("AKI")

    assertThat(prisonDetails)
      .extracting("prisonName").isEqualTo("Acklington (HMP)")
  }

  @Test
  fun savingPrisonDetails() {
    val before = repository.findAll()

    repository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    repository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    repository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))

    val after = repository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(3)
    assertThat(before).isEqualTo(after)
  }

  @Test
  fun savingPrisonDetailsNewPrison() {
    val before = repository.findAll()

    repository.save(PrisonDetail(prisonId = "AKI", prisonName = "Acklington (HMP)"))
    repository.save(PrisonDetail(prisonId = "ALI", prisonName = "Albany (HMP)"))
    repository.save(PrisonDetail(prisonId = "ANI", prisonName = "Aldington (HMP)"))
    repository.save(PrisonDetail(prisonId = "MDI", prisonName = "Moorland (HMP & YOI)"))

    val after = repository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(4)
  }
}
