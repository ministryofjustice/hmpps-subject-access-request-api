package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.LocationsApiExtension.Companion.locationsApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.integration.wiremock.NomisMappingsApiExtension.Companion.nomisMappingsApi
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.LocationDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.LocationDetailsRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed.UpdateLocationNameDataService

@ActiveProfiles("test")
class LocationsIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var updateLocationNameDataService: UpdateLocationNameDataService

  @Autowired
  private lateinit var locationDetailsRepository: LocationDetailsRepository

  @Test
  fun `Location names are updated in repository`() {
    locationsApi.stubGetLocationDetails()
    nomisMappingsApi.stubLocationMappings()

    updateLocationNameDataService.updateLocationData()

    assertThat(locationDetailsRepository.findAll())
      .containsExactlyInAnyOrder(
        LocationDetail(dpsId = "00000be5-081c-4374-8214-18af310d3d4a", nomisId = 80065, name = "PROPERTY BOX 27"),
        LocationDetail(dpsId = "000047c0-38e1-482e-8bbc-07d4b5f57e23", nomisId = 389406, name = "B WING"),
        LocationDetail(dpsId = "00041fb1-4710-476f-ada8-3ea7e8f2ae50", nomisId = 167792, name = "MARLBOROUGH EXERCISE"),
        LocationDetail(dpsId = "0004bd05-edb2-473b-bc39-f94c6ebe3b0b", nomisId = 165542, name = "VALUABLES"),
      )
  }
}
