# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository.

## Project overview

`hmpps-subject-access-request-api` is a Spring Boot application written in Kotlin. It acts as an
orchestration layer between the Subject Access Request front end and external APIs such as the
document storage service, prisoner-search, HMPPS Auth, and the services data is requested from.
It is used by the [Subject Access Request UI](https://github.com/ministryofjustice/hmpps-subject-access-request-ui)
and the [Subject Access Request worker](https://github.com/ministryofjustice/hmpps-subject-access-request-worker).

## Tech stack

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

# Run Ktlint check
./gradlew ktlintCheck

# List dependencies / check for updates
./gradlew dependencies
./gradlew dependencyUpdates --warning-mode all

# OWASP dependency check
./gradlew clean dependencyCheckAnalyze --info
```

Run the smallest relevant command for a change (e.g. a single test class) where possible, and run
`./gradlew ktlintCheck` and `./gradlew test` before considering Kotlin changes complete.

## Running locally

1. Start local dependencies: `docker-compose up -d`
2. Add `username`/`password` (API_CLIENT_ID / API_CLIENT_SECRET) to the `hmpps-auth` section of
   `application-local.yaml`.
3. Run the app with Spring profile `local` from your IDE, or via Gradle.

## Conventions

- Kotlin code style is enforced by Ktlint — run `./gradlew ktlintCheck` after edits and fix any
  violations (`./gradlew ktlintFormat` can auto-fix most issues).
- Follow the existing package-by-layer structure described above when adding new code.
- Add/modify Flyway migrations under `db/migration` (PostgreSQL) — don't edit already-applied
  migrations; add a new versioned migration file instead. Keep `migration_h2` in sync if required
  for tests.
- Prefer updating or adding tests alongside code changes; tests live under `src/test/kotlin`
  mirroring the main package structure.
- Don't commit secrets; local secrets for `hmpps-auth` come from Kubernetes dev namespace secrets.

## Pull requests

- Keep changes focused and avoid unrelated refactors.
- Ensure `./gradlew ktlintCheck` and `./gradlew test` pass before opening a PR.
