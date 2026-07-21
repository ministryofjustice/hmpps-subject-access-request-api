plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.1"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyCheck {
  suppressionFiles.add("httpcore-dependency-check-suppress.xml")
}

// Temporarily pin spring doc at 3.0.2 whilst waiting for 3.0.4 upgrade
val springDocVersion = "3.0.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.0")
  implementation("org.json:json:20260719")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
  constraints {
    implementation("org.webjars:swagger-ui:5.32.2")
  }
  implementation("io.sentry:sentry-spring-boot-4:8.49.0")
  implementation("io.sentry:sentry-logback:8.49.0")

  implementation("uk.gov.service.notify:notifications-java-client:6.0.1-RELEASE")
  implementation("net.javacrumbs.shedlock:shedlock-spring:7.7.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.7.0")
  implementation("com.slack.api:slack-api-client:1.49.0")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-lib:2.6.2")
  implementation("commons-io:commons-io:2.22.0")
  implementation("com.google.guava:guava:33.6.0-jre")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("com.h2database:h2:2.4.240")
  runtimeOnly("org.postgresql:postgresql:42.7.13")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.0-beta2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}
