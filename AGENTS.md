# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository.

## Project overview

`hmpps-subject-access-request-api` is a Spring Boot application written in Kotlin. It acts as an
orchestration layer between the Subject Access Request front end and external APIs such as the
document storage service, prisoner-search, HMPPS Auth, and the services data is requested from.
It is used by the [Subject Access Request UI](https://github.com/ministryofjustice/hmpps-subject-access-request-ui)
and the [Subject Access Request worker](https://github.com/ministryofjustice/hmpps-subject-access-request-worker).

## Tech stack

- JAVA 25
- Kotlin / Spring Boot (`uk.gov.justice.hmpps.gradle-spring-boot` plugin)
- Gradle (Kotlin DSL) build via `./gradlew`
- Java version pinned in `.java-version`
- PostgreSQL (prod/dev) with Flyway migrations; H2 used for tests
- Docker / docker-compose for local dependencies
- Deployed to Kubernetes via Helm charts in `helm_deploy/`

## Repository layout

- `src/main/kotlin/uk/gov/justice/digital/hmpps/subjectaccessrequestapi/`
  - `client/` – clients for external services
  - `config/` – Spring configuration
  - `controllers/` – REST controllers (`entity/` for request/response DTOs)
  - `exceptions/` – error handling
  - `health/` – health indicators
  - `models/` – domain models
  - `repository/` – Spring Data repositories
  - `services/` – business logic
  - `timed/` – scheduled jobs (`alerts/` subpackage)
  - `utils/` – helpers
- `src/main/resources/db/migration` – PostgreSQL Flyway migrations (`migration_h2` for test DB)
- `src/test/` – Kotlin tests (JUnit5)
- `wiremock/` – stub mappings used by tests/local run
- `helm_deploy/` – Kubernetes Helm chart
- `.github/workflows/` – CI/CD pipelines (uses shared `ministryofjustice/hmpps-github-actions` workflows)

## Build, test, and lint commands

```bash
# Build without tests
./gradlew clean build -x test

# Run tests
./gradlew test

# List dependencies / check for updates
./gradlew dependencies
./gradlew dependencyUpdates --warning-mode all

# OWASP dependency check
./gradlew clean dependencyCheckAnalyze --info
```

Run the smallest relevant command for a change (e.g. a single test class) where possible, and run
`./gradlew test` before considering Kotlin changes complete.

## Running locally

1. Start local dependencies: `docker-compose up -d`
2. Add `username`/`password` (API_CLIENT_ID / API_CLIENT_SECRET) to the `hmpps-auth` section of
   `application-local.yaml`.
3. Run the app with Spring profile `local` from your IDE, or via Gradle.

## Conventions

- Follow the existing Kotlin code style. Do not run any Gradle ktlint task in this repository,
  including `ktlintCheck` and `ktlintFormat`.
- Follow the existing package-by-layer structure described above when adding new code.
- Add/modify Flyway migrations under `db/migration` (PostgreSQL) — don't edit already-applied
  migrations; add a new versioned migration file instead. Keep `migration_h2` in sync if required
  for tests.
- Prefer updating or adding tests alongside code changes; tests live under `src/test/kotlin`
  mirroring the main package structure.
- Don't commit secrets; local secrets for `hmpps-auth` come from Kubernetes dev namespace secrets.

## Pull requests

- Keep changes focused and avoid unrelated refactors.
- Ensure `./gradlew test` passes before opening a PR. Do not run any Gradle ktlint task.

## Dependency injection

Prefer constructor injection over field injection in Spring components, including test classes.

- In production code, declare dependencies as constructor parameters (e.g. `private val fooRepository: FooRepository`) rather than using `@Autowired` on a field.
- In integration test classes, pass `@Autowired` repositories/beans as primary constructor parameters instead of using `@Autowired` on a `lateinit var` field.

## Tests

Prefer `@BeforeAll`/`@AfterAll` over `@BeforeEach`/`@AfterEach` where possible in nested integration test classes.

- Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` on the nested test class so that `@BeforeAll`/`@AfterAll` can be non-static (instance) methods.
- Build shared fixture data once in `@BeforeAll` and clean it up once in `@AfterAll`, rather than rebuilding it before every test, when the fixture is not mutated in a way that would leak between tests.
- When copying or adapting an integration test, preserve the full nested test structure, including `Security`, `Validation`, and `HappyPath` scenarios for each endpoint.
- Place each `@AfterAll` method immediately after the corresponding `@BeforeAll` setup method.
- OAuth2 propagation tests must verify the exact WireMock header value with `equalTo("Bearer ABCDE")`; do not weaken this assertion to a regex or non-empty-header matcher.

## Linting

Do not run `ktlintCheck` (or any ktlint gradle task) in this repository. Skip ktlint validation entirely.
