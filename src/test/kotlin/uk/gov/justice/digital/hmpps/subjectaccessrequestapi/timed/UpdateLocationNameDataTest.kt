package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationResults
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.NomisLocationMapping
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.NomisMappingsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.LocationDetail

private const val DPS_ID_ONE = "00000be5-081c-4374-8214-18af310d3d4a"
private const val DPS_ID_TWO = "000047c0-38e1-482e-8bbc-07d4b5f57e23"
private const val DPS_ID_THREE = "00041fb1-4710-476f-ada8-3ea7e8f2ae50"
private const val DPS_ID_FOUR = "0004bd05-edb2-473b-bc39-f94c6ebe3b0b"

private const val NOMIS_ID_ONE = 389406
private const val NOMID_ID_TWO = 80065
private const val NOMIS_ID_THREE = 167792
private const val NOMID_ID_FOUR = 165542

class UpdateLocationNameDataTest {

  private val locationDetailsRepositoryProxy: LocationDetailsRepositoryProxy = mock()
  private val locationsClient: LocationsClient = mock()
  private val nomisMappingsClient: NomisMappingsClient = mock()

  private val updateLocationNameDataService = UpdateLocationNameDataService(locationDetailsRepositoryProxy, locationsClient, nomisMappingsClient)

  private val locationClientResults = listOf(
    LocationDetails(id = DPS_ID_ONE, localName = "PROPERTY BOX 27", pathHierarchy = "PROP_BOXES-PB027"),
    LocationDetails(id = DPS_ID_TWO, localName = "B WING", pathHierarchy = "RES-HB3-BWING"),
    LocationDetails(id = DPS_ID_THREE, localName = null, pathHierarchy = "B-2-008"),
    LocationDetails(id = DPS_ID_FOUR, localName = "VALUABLES", pathHierarchy = "PROP_VALS"),
  )

  private val nomislocationMappings = listOf(
    NomisLocationMapping(dpsLocationId = DPS_ID_ONE, nomisLocationId = NOMIS_ID_ONE),
    NomisLocationMapping(dpsLocationId = DPS_ID_TWO, nomisLocationId = NOMID_ID_TWO),
    NomisLocationMapping(dpsLocationId = DPS_ID_THREE, nomisLocationId = NOMIS_ID_THREE),
    NomisLocationMapping(dpsLocationId = DPS_ID_FOUR, nomisLocationId = NOMID_ID_FOUR),
  )

  @AfterEach
  fun verifyNoMoreInteractions() {
    verifyNoMoreInteractions(locationsClient, nomisMappingsClient, locationDetailsRepositoryProxy)
  }

  @Test
  fun `should update location name data when none already exist`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 1, locationClientResults))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(emptyList<LocationDetail>())
    whenever(nomisMappingsClient.getNomisLocationMappings(any())).thenReturn(nomislocationMappings)

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO, DPS_ID_THREE, DPS_ID_FOUR))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_ONE, DPS_ID_TWO, DPS_ID_THREE, DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008"),
        LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES"),
      ),
    )
  }

  @Test
  fun `should update location name data when all already exist`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 1, locationClientResults))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "EXISTING VALUE"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "EXISTING VALUE 2"),
        LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES ORIGINAL"),
      ),
    )

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO, DPS_ID_THREE, DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008"),
        LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES"),
      ),
    )
  }

  @Test
  fun `should update location name data when some already exist`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 1, locationClientResults))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "EXISTING VALUE"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008"),
      ),
    )
    whenever(nomisMappingsClient.getNomisLocationMappings(any())).thenReturn(listOf(nomislocationMappings[1], nomislocationMappings[3]))

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO, DPS_ID_THREE, DPS_ID_FOUR))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_TWO, DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008"),
        LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES"),
      ),
    )
  }

  @Test
  fun `should update location name data when mappings not found`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 0, locationClientResults))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "EXISTING VALUE"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "EXISTING VALUE 2"),
      ),
    )
    whenever(nomisMappingsClient.getNomisLocationMappings(any())).thenReturn(listOf(nomislocationMappings[1]))

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO, DPS_ID_THREE, DPS_ID_FOUR))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_TWO, DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
        LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008"),
        LocationDetail(DPS_ID_FOUR, null, "VALUABLES"),
      ),
    )
  }

  @Test
  fun `should abandon when error getting location details on first call`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(null)

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
  }

  @Test
  fun `should update location name data when multiple pages`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 3, listOf(locationClientResults[0], locationClientResults[1])))
    whenever(locationsClient.getLocationDetails(1)).thenReturn(LocationResults(totalPages = 3, listOf(locationClientResults[2])))
    whenever(locationsClient.getLocationDetails(2)).thenReturn(LocationResults(totalPages = 3, listOf(locationClientResults[3])))
    whenever(nomisMappingsClient.getNomisLocationMappings(listOf(DPS_ID_ONE, DPS_ID_TWO))).thenReturn(listOf(nomislocationMappings[0], nomislocationMappings[1]))
    whenever(nomisMappingsClient.getNomisLocationMappings(listOf(DPS_ID_THREE))).thenReturn(listOf(nomislocationMappings[2]))
    whenever(nomisMappingsClient.getNomisLocationMappings(listOf(DPS_ID_FOUR))).thenReturn(listOf(nomislocationMappings[3]))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(emptyList<LocationDetail>())

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationsClient).getLocationDetails(1)
    verify(locationsClient).getLocationDetails(2)
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_ONE, DPS_ID_TWO))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_THREE))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO))
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_THREE))
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
      ),
    )
    verify(locationDetailsRepositoryProxy).updateLocationData(listOf(LocationDetail(DPS_ID_THREE, NOMIS_ID_THREE, "B-2-008")))
    verify(locationDetailsRepositoryProxy).updateLocationData(listOf(LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES")))
  }

  @Test
  fun `should update location name data when multiple pages and one location call fails`() {
    whenever(locationsClient.getLocationDetails(0)).thenReturn(LocationResults(totalPages = 3, listOf(locationClientResults[0], locationClientResults[1])))
    whenever(locationsClient.getLocationDetails(1)).thenReturn(null)
    whenever(locationsClient.getLocationDetails(2)).thenReturn(LocationResults(totalPages = 3, listOf(locationClientResults[3])))
    whenever(nomisMappingsClient.getNomisLocationMappings(listOf(DPS_ID_ONE, DPS_ID_TWO))).thenReturn(listOf(nomislocationMappings[0], nomislocationMappings[1]))
    whenever(nomisMappingsClient.getNomisLocationMappings(listOf(DPS_ID_FOUR))).thenReturn(listOf(nomislocationMappings[3]))
    whenever(locationDetailsRepositoryProxy.getLocationDetails(any())).thenReturn(emptyList<LocationDetail>())

    updateLocationNameDataService.updateLocationData()

    verify(locationsClient).getLocationDetails(0)
    verify(locationsClient).getLocationDetails(1)
    verify(locationsClient).getLocationDetails(2)
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_ONE, DPS_ID_TWO))
    verify(nomisMappingsClient).getNomisLocationMappings(listOf(DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_ONE, DPS_ID_TWO))
    verify(locationDetailsRepositoryProxy).getLocationDetails(listOf(DPS_ID_FOUR))
    verify(locationDetailsRepositoryProxy).updateLocationData(
      listOf(
        LocationDetail(DPS_ID_ONE, NOMIS_ID_ONE, "PROPERTY BOX 27"),
        LocationDetail(DPS_ID_TWO, NOMID_ID_TWO, "B WING"),
      ),
    )
    verify(locationDetailsRepositoryProxy).updateLocationData(listOf(LocationDetail(DPS_ID_FOUR, NOMID_ID_FOUR, "VALUABLES")))
  }
}
