package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.services

import io.jsonwebtoken.Jwts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

  fun setAuthorisation(
    user: String = "subject-access-request-1",
    roles: List<String> = listOf(),
    name: String,
  ): (HttpHeaders) -> Unit {
    val token = createJwt(
      subject = user,
      scope = listOf("read"),
      expiryTime = Duration.ofHours(1L),
      roles = roles,
      name = name,
    )
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun createJwt(
    subject: String? = null,
    name: String? = null,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String {
    val claims = mutableMapOf<String, Any?>("client_id" to "subject-access-request-1")
      .apply {
        subject?.let { this["user_name"] = subject }
        roles?.let { this["authorities"] = roles }
        name?.let { this["sub"] = name }
        scope?.let { this["scope"] = scope }
      }
    return Jwts.builder()
      .id(jwtId)
      .subject(subject)
      .claims(claims)
      .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(keyPair.private, Jwts.SIG.RS256)
      .compact()
  }
}
