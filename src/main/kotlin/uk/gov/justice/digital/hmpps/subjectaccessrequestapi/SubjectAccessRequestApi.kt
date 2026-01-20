package uk.gov.justice.digital.hmpps.subjectaccessrequestapi

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@SpringBootApplication()
class SubjectAccessRequestApi

fun main(args: Array<String>) {
  runApplication<SubjectAccessRequestApi>(*args)
}

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT60S")
class SchedulingConfiguration {
  @Bean
  fun lockProvider(dataSource: DataSource): LockProvider? = JdbcTemplateLockProvider(dataSource)
}
