package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.timed

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  private val locationDetailsRepository: LocationDetailsRepository,
  private val locationsClient: LocationsClient,
  private val nomisMappingsClient: NomisMappingsClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updateLocationData() {
    log.info("updating location details in database")

    var pageNumber = 0
    do {
      val (last, dpsLocations) = locationsClient.getLocationDetails(pageNumber)
      val storedLocationsMap = locationDetailsRepository.findAllByDpsIdIn(dpsLocations.map { it.id }).map { it.dpsId to it }.toMap()
      val missingDpsLocations = dpsLocations.filter { it.id !in storedLocationsMap.keys }
      val missingNomisLocationMap =
        if (missingDpsLocations.isEmpty()) {
          emptyMap<String, Int>()
        } else {
          nomisMappingsClient.getNomisLocationMappings(missingDpsLocations.map { it.id }).map { it.dpsLocationId to it.nomisLocationId }.toMap()
        }

      dpsLocations.forEach {
        val nomisId = storedLocationsMap[it.id]?.nomisId ?: missingNomisLocationMap[it.id]
        locationDetailsRepository.save(LocationDetail(dpsId = it.id, nomisId = nomisId, name = it.localName))
      }

      pageNumber++
    } while (!last)

    log.info("location details updated in database")
  }
}
