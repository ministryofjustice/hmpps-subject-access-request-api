package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("hmppsAuth")
class HmppsAuthHealthPing(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("prisonRegister")
class PrisonRegisterHealthPing(@Qualifier("prisonRegisterWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("nomisUserRolesApi")
class NOMISHealthPing(@Qualifier("nomisUserRolesApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("sarAndDeliusApi")
class SarAndDeliusHealthPing(@Qualifier("sarAndDeliusApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
