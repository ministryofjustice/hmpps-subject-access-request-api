package uk.gov.justice.digital.hmpps.hmppssubjectaccessrequestapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
  @Bean
  fun customConfiguration(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://subject-access-request-prod.hmpps.service.justice.gov.uk")
          .description("Production"),
        Server().url("https://subject-access-request-preprod.hmpps.service.justice.gov.uk")
          .description("Preproduction"),
        Server().url("https://subject-access-request-dev.hmpps.service.justice.gov.uk")
          .description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("HMPPS Subject Access Request API Documentation")
        .description(this.javaClass.getResource("/documentation/service-description.html")!!.readText())
        .license(License().name("MIT").url("https://opensource.org/licenses/MIT")),
    )
}
