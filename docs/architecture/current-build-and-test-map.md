# Current Build And Test Map

## Status

Slice 00 baseline for the microservices ecosystem conversion workflow.

This document records the verified build, test, quality and deployment-command
state before service migration begins. It does not claim that any quality gate
passed unless the command is explicitly listed as executed.

## Build Baseline

| Item | Verified State |
|---|---|
| Root build | Gradle Kotlin DSL multi-project build |
| Gradle wrapper | `9.4.0` from `gradle/wrapper/gradle-wrapper.properties` |
| Java toolchain | Java 25 from root `build.gradle.kts` |
| JUnit | JUnit 6.0.3 from `gradle/libs.versions.toml` |
| ArchUnit | 1.4.1 from `gradle/libs.versions.toml` |
| JaCoCo | 0.8.14 from `gradle/libs.versions.toml` |
| Dependency verification | `gradle/verification-metadata.xml`, strict mode required by `QUALITY.md` |
| Repository CI | No `.github/workflows` directory verified |

The root build applies Java and JaCoCo to subprojects, configures Java 25
compilation with `-Xlint:all`, runs tests on the JUnit Platform, and configures
JaCoCo XML and HTML reports.

## Included Gradle Modules

Verified from `settings.gradle.kts`:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `forensic-analytics-logging`
- `forensic-analytics-observability`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-cli`
- `forensic-analytics-testbed`
- `forensic-analytics-persistence`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-rest`
- `forensic-analytics-bootstrap`
- `forensic-analytics-boot-app`

No service-specific Gradle projects under `services/**` exist yet.

## Quality Commands

Minimum command from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate from `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Diagnostic command from `QUALITY.md`:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

`./gradlew clean check` is not a complete replacement for the full local gate
unless it is later verified to include `checkPackageCoverage`.

`validatePlugins` is documented for plugin metadata, Gradle task input/output
or plugin implementation changes only. Slice 00 does not change those areas.

## Coverage Configuration

Root `build.gradle.kts` configures:

- `jacocoTestReport` with XML and HTML reports;
- `jacocoTestCoverageVerification` with a 0.80 minimum;
- root `checkPackageCoverage`, which reads JaCoCo XML reports and enforces
  80% package line coverage and 80% package branch coverage where branch data
  exists.

The full local gate must call `checkPackageCoverage` explicitly.

## Current Test Layout

Verified Java test sources exist under module-local `src/test/java` trees.
The test suite covers domain, application, adapters, gRPC ingestion,
persistence, REST, CLI, bootstrap, Boot, logging, observability and testbed
scenarios.

Architecture tests were verified in these areas:

- application contract architecture;
- ingestion gRPC architecture;
- REST architecture;
- logging architecture;
- observability architecture;
- Spring Boot architecture through testbed coverage.

No `src/integrationTest`, `src/functionalTest` or `src/e2e` source sets were
verified in the current Gradle build.

No repository-configured mutation-testing task was verified.

## Frontend Build And Test

The verified frontend root is `forensic-ui`.

Scripts from `forensic-ui/package.json`:

```bash
npm run dev
npm run build
npm run preview
npm test
npm run test:watch
```

Frontend tests use Vitest with jsdom and Testing Library. No frontend lint,
coverage, Playwright or Cypress command was verified.

The planned `frontend/frontend-web-app` root exists only as a Slice 02
placeholder. It has no frontend implementation, package metadata, build
configuration or tests.

## Docker And Deployment Verification

Existing Docker material:

- `docker/boot-app/Dockerfile`
- `docker/boot-app/README.md`
- `docker/joern/Dockerfile`
- `docker/joern/docker-compose.joern.yml`
- `docker/joern/scripts/**`
- `forensic-ui/Dockerfile`

The DevOps review verified Joern Compose configuration with:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config
```

No Boot jar build, Boot Docker image build, frontend image build, Swarm command
or Kubernetes command was executed as part of Slice 00.

Missing deployment material:

- `deployment/docker-compose/docker-compose.yml`
- `deployment/docker-swarm/stack.yml`
- `deployment/kubernetes/**`
- Helm or chart roots
- service-local Dockerfiles under `services/**`
- service healthcheck definitions

## Slice 00 Verification Rule

Slice 00 is documentation-only. The required verification is:

```bash
git diff --check
```

If a later change in this slice modifies production code, build logic, tests or
deployment behavior, run the minimum `QUALITY.md` command before continuing:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

No current service-specific test command exists because no service projects
exist yet.

## Residual Risks

- CI workflow evidence is absent.
- Service-specific test tasks do not exist yet.
- Contract tests do not exist yet.
- Frontend coverage and end-to-end test gates are not configured.
- Boot container healthcheck evidence is absent.
- Swarm and Kubernetes readiness is absent.
- Full local quality gate has not been run for Slice 00 because this slice is
  documentation-only and requires `git diff --check`.
