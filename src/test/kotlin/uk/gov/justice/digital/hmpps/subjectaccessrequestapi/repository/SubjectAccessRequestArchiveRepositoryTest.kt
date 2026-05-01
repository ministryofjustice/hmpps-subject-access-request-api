package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.ArchiveExpiredRequestsTestFixture
import java.time.LocalDateTime

@DataJpaTest
class SubjectAccessRequestArchiveRepositoryTest : ArchiveExpiredRequestsTestFixture() {

  @Autowired
  private lateinit var repository: SubjectAccessRequestArchiveRepository

  @Nested
  inner class Save {

    @Test
    fun `should insert archived request`() {
      assertThat(repository.findAll()).isEmpty()

      repository.saveAndFlush(archivedSAR_1_1)

      val result = repository.findAll()
      assertThat(result).hasSize(1)
      assertThat(result.first()).isEqualTo(archivedSAR_1_1)
    }
  }

  @Nested
  inner class FindBySarIdAndServiceName {

    @Test
    fun `findBySarIdAndServiceName should return null when no result matches`() {
      assertThat(repository.findAll()).isEmpty()
      repository.saveAndFlush(archivedSAR_1_1)

      val result = repository.findBySarIdAndServiceName(archivedSAR_1_1.sarId, archivedSAR_1_1.serviceName)
      assertThat(result).isEqualTo(archivedSAR_1_1)
    }
  }

  @Nested
  inner class DeleteBySarRequestDateTimeBefore {

    @Test
    fun `should delete archived requests created after cut off`() {
      val archivedRequest = archiveRequestWithRequestDateTime(dateTimeNow.minusYears(2))

      repository.saveAndFlush(archivedRequest)
      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNotNull

      repository.deleteBySarRequestDateTimeBefore(LocalDateTime.now().minusYears(1))

      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNull()
    }

    @Test
    fun `should not delete archived requests created after cut off`() {
      val archivedRequest = archiveRequestWithRequestDateTime(dateTimeNow.minusDays(364))

      repository.saveAndFlush(archivedRequest)
      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNotNull

      repository.deleteBySarRequestDateTimeBefore(dateTimeNow.minusDays(365))

      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNotNull
    }

    @Test
    fun `should not delete archived requests created is less than or equal to cut off`() {
      val archivedRequest = archiveRequestWithRequestDateTime(dateTimeNow.minusDays(365))

      repository.saveAndFlush(archivedRequest)
      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNotNull

      repository.deleteBySarRequestDateTimeBefore(dateTimeNow.minusDays(365))

      assertThat(repository.findByIdOrNull(archivedRequest.id)).isNotNull
    }

    @Test
    fun `should only delete archived requests requested after cut off`() {
      val archivedRequest1 = archiveRequestWithRequestDateTime(dateTimeNow.minusDays(366))
      val archivedRequest2 = archiveRequestWithRequestDateTime(dateTimeNow.minusDays(364))

      repository.saveAllAndFlush(listOf(archivedRequest1, archivedRequest2))
      assertThat(repository.findByIdOrNull(archivedRequest1.id)).isNotNull
      assertThat(repository.findByIdOrNull(archivedRequest2.id)).isNotNull

      repository.deleteBySarRequestDateTimeBefore(dateTimeNow.minusYears(1))

      assertThat(repository.findByIdOrNull(archivedRequest1.id)).isNull()
      assertThat(repository.findByIdOrNull(archivedRequest2.id)).isNotNull
    }
  }
}
