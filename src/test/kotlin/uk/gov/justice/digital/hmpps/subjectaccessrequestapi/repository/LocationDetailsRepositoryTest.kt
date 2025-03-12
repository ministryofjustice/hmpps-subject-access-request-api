package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.LocationDetail
import java.util.stream.Stream

private const val DPS_ID_ONE = "eb50e14d-bb48-4269-aadb-9af43207fad3"
private const val DPS_ID_TWO = "69529399-458c-484c-b62c-7af1660941b7"
private const val DPS_ID_THREE = "b9fa64b8-fcf1-4c73-b401-3dddfe32680d"

@DataJpaTest
class LocationDetailsRepositoryTest {

  companion object {
    val LOCATION_DETAIL_ONE = LocationDetail(dpsId = DPS_ID_ONE, nomisId = 1234567, name = "PROPERTY BOX 27")
    val LOCATION_DETAIL_TWO = LocationDetail(dpsId = DPS_ID_TWO, nomisId = 2345678, name = "B WING")
    val LOCATION_DETAIL_THREE = LocationDetail(dpsId = DPS_ID_THREE, nomisId = 3456789, name = "CELL")

    @JvmStatic
    fun findAllByDpsId(): Stream<Arguments> = Stream.of(
      arguments(emptyList<String>(), emptyList<LocationDetails>()),
      arguments(listOf("non-existing-id"), emptyList<LocationDetails>()),
      arguments(listOf(DPS_ID_TWO), listOf(LOCATION_DETAIL_TWO)),
      arguments(listOf(DPS_ID_ONE, DPS_ID_THREE), listOf(LOCATION_DETAIL_ONE, LOCATION_DETAIL_THREE)),
      arguments(listOf(DPS_ID_ONE, DPS_ID_TWO, "non-existing-id"), listOf(LOCATION_DETAIL_ONE, LOCATION_DETAIL_TWO)),
    )
  }

  @Autowired
  lateinit var locationDetailsRepository: LocationDetailsRepository

  @BeforeEach
  fun setup() {
    locationDetailsRepository.deleteAll()
    locationDetailsRepository.save(LOCATION_DETAIL_ONE)
    locationDetailsRepository.save(LOCATION_DETAIL_TWO)
    locationDetailsRepository.save(LOCATION_DETAIL_THREE)
  }

  @ParameterizedTest
  @MethodSource("findAllByDpsId")
  fun testFindAllByDpsIdIn(dpsIds: List<String>, expectedLocationDetails: List<LocationDetail>) {
    val locationDetails = locationDetailsRepository.findAllByDpsIdIn(dpsIds)

    assertThat(locationDetails).containsExactlyInAnyOrderElementsOf(expectedLocationDetails)
  }

  @Test
  fun savingLocationDetails() {
    val before = locationDetailsRepository.findAll()

    locationDetailsRepository.save(LOCATION_DETAIL_ONE)
    locationDetailsRepository.save(LOCATION_DETAIL_TWO)
    locationDetailsRepository.save(LOCATION_DETAIL_THREE)

    val after = locationDetailsRepository.findAll()

    assertThat(before).hasSize(3)
    assertThat(after).hasSize(3)
    assertThat(before).isEqualTo(after)
  }

  @Test
  fun savingLocationDetailsNewLocation() {
    val newLocation = LocationDetail(dpsId = "fd9f67c3-e558-4b86-9b6c-a2ee879e5b06", nomisId = 4567890, name = "PROP BOX")
    val before = locationDetailsRepository.findAll()

    locationDetailsRepository.save(LOCATION_DETAIL_ONE)
    locationDetailsRepository.save(LOCATION_DETAIL_TWO)
    locationDetailsRepository.save(LOCATION_DETAIL_THREE)
    locationDetailsRepository.save(newLocation)

    val after = locationDetailsRepository.findAll()

    assertThat(before).containsExactlyInAnyOrder(LOCATION_DETAIL_ONE, LOCATION_DETAIL_TWO, LOCATION_DETAIL_THREE)
    assertThat(after).containsExactlyInAnyOrder(LOCATION_DETAIL_ONE, LOCATION_DETAIL_TWO, LOCATION_DETAIL_THREE, newLocation)
  }
}
