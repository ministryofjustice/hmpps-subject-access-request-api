package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.ArchiveExpiredRequestsTestFixture

@DataJpaTest
class SubjectAccessRequestArchiveRepositoryTest : ArchiveExpiredRequestsTestFixture() {

  @Autowired
  private lateinit var repository: SubjectAccessRequestArchiveRepository

  @Test
  fun `should insert archived request`() {
    assertThat(repository.findAll()).isEmpty()

    repository.saveAndFlush(archivedSAR_1_1)

    val result = repository.findAll()
    assertThat(result).hasSize(1)
    assertThat(result.first()).isEqualTo(archivedSAR_1_1)
  }

  @Test
  fun `findBySarIdAndServiceName should return null when no result matches`() {
    assertThat(repository.findAll()).isEmpty()
    repository.saveAndFlush(archivedSAR_1_1)

    val result = repository.findBySarIdAndServiceName(archivedSAR_1_1.sarId, archivedSAR_1_1.serviceName)
    assertThat(result).isEqualTo(archivedSAR_1_1)
  }
}
