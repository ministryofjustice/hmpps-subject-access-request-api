package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationDetails
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.LocationsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.client.NomisMappingsClient
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.models.LocationDetail
import uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository.LocationDetailsRepository

/**
 * Refresh location cache weekly = 604800000 milliseconds
 */
@Component
class UpdateLocationNameData(private val service: UpdateLocationNameDataService) {

  @Scheduled(
    fixedDelayString = "\${application.location-refresh.frequency}",
    initialDelayString = "\${random.int[60000,\${application.location-refresh.frequency}]}",
  )
  fun updateLocationCache() {
    try {
      service.updateLocationData()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during location cache update", e.javaClass.simpleName, e)
      Sentry.captureException(e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class UpdateLocationNameDataService(
  private val locationDetailsRepositoryProxy: LocationDetailsRepositoryProxy,
  private val locationsClient: LocationsClient,
  private val nomisMappingsClient: NomisMappingsClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun updateLocationData() {
    log.info("updating location details in database")

    var pageNumber = 0
    var totalPages = 1
    do {
      log.info("updating location details for page {}", pageNumber)
      locationsClient.getLocationDetails(pageNumber)?.let { locationResults ->
        totalPages = locationResults.totalPages
        val dpsLocations = locationResults.content
        val dpsLocationIds = dpsLocations.map { it.id }
        val storedLocationsMap = locationDetailsRepositoryProxy.getLocationDetails(dpsLocationIds).map { it.dpsId to it }.toMap()
        val missingNomisLocationMap = getMissingNomisMappings(dpsLocationIds, storedLocationsMap)
        val locationDetailsToSave = getLocationDetailsToSave(dpsLocations, storedLocationsMap, missingNomisLocationMap)
        locationDetailsRepositoryProxy.updateLocationData(locationDetailsToSave)
      }
      pageNumber++
    } while (pageNumber < totalPages)

    log.info("finished updating location details in database")
  }

  private fun getLocationDetailsToSave(
    dpsLocations: List<LocationDetails>,
    storedLocationsMap: Map<String, LocationDetail>,
    missingNomisLocationMap: Map<String, Int>,
  ): List<LocationDetail> = dpsLocations.map {
    val nomisId = storedLocationsMap[it.id]?.nomisId ?: missingNomisLocationMap[it.id]
    LocationDetail(dpsId = it.id, nomisId = nomisId, name = it.localName ?: it.pathHierarchy)
  }

  private fun getMissingNomisMappings(
    dpsLocationIds: List<String>,
    storedLocationsMap: Map<String, LocationDetail>,
  ): Map<String, Int> = dpsLocationIds.filter { it !in storedLocationsMap.keys }.let { missingDpsLocationIds ->
    if (missingDpsLocationIds.isEmpty()) {
      emptyMap<String, Int>()
    } else {
      nomisMappingsClient.getNomisLocationMappings(missingDpsLocationIds)
        .map { it.dpsLocationId to it.nomisLocationId }.toMap()
    }
  }
}

@Service
class LocationDetailsRepositoryProxy(
  private val locationDetailsRepository: LocationDetailsRepository,
) {

  @Transactional
  fun updateLocationData(locations: List<LocationDetail>) {
    locations.forEach { locationDetailsRepository.save(it) }
  }

  fun getLocationDetails(dpsIds: List<String>): List<LocationDetail> = locationDetailsRepository.findAllByDpsIdIn(dpsIds)
}
