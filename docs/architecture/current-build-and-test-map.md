# Current Build And Test Map

## Status

Slice 00 baseline for the microservices ecosystem conversion workflow.

This document records the verified build, test, quality and deployment-command
state before service migration begins, then appends verified slice updates as
target services are introduced. It does not claim that any quality gate passed
unless the command is explicitly listed as executed.

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
- `services:btm-generation-service`
- `services:joern-cpg-analysis-service`
- `services:joern-analysis-service`
- `services:analysis-orchestrator-service`
- `services:java-parser-analysis-service`
- `services:java-ast-analysis-service`
- `services:repository-source-service`
- `services:repository-analysis-service`
- `services:analysis-store-service`
- `services:ingestion-service`
- `services:forensic-ingestion-service`
- `services:forensic-gateway-service`

Twelve service-specific Gradle projects under `services/**` are now registered.
`services:repository-source-service`, `services:ingestion-service` and
`services:java-parser-analysis-service`, `services:joern-analysis-service` and
`services:analysis-orchestrator-service` are FA-MSA-001 target-name service
projects introduced by this workflow.
`services:repository-analysis-service`, `services:forensic-ingestion-service`,
`services:java-ast-analysis-service` and `services:joern-cpg-analysis-service`
remain predecessor services and rollback inputs, not compatibility aliases.
Graph-replay and report-generation remain README-only planned service roots and
are explicitly deferred from repository-to-BTM acceptance by Slice 16. The
build-artifact worker is planned by workflow v2 and retained by workflow v3; it
has no service root yet.

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

`forensic-analytics-testbed` now includes a deterministic local real
repository E2E fixture under `src/test/resources/repository-e2e/`. The
corresponding `RepositoryAnalysisRealRepositoryEndToEndTest` materializes that
fixture as a local Git repository, pins a commit, sends a plugin-style in-process
gRPC request and verifies checkout, source-root detection, session storage and
workspace cleanup without external network access or target build execution.

WildFly large-repository hardening is available as an opt-in testbed scenario
through `WildFlyRepositoryHardeningTest`. It is skipped by default unless
`FORENSIC_ANALYTICS_WILDFLY_HARDENING=true` and an explicit WildFly branch or
commit is provided. The runbook is `docs/testing/wildfly-hardening.md`.

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

The planned `frontend/frontend-web-app` root exists only as a
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
- `services/repository-source-service/Dockerfile`
- `services/ingestion-service/Dockerfile`
- `services/java-parser-analysis-service/Dockerfile`
- `services/joern-analysis-service/Dockerfile`
- `services/analysis-orchestrator-service/Dockerfile`
- `deployment/docker-compose/repository-to-btm.local.yml`

The DevOps review verified Joern Compose configuration with:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config
```

No Boot jar build, Boot Docker image build, frontend image build, Swarm command
or Kubernetes command was executed as part of Slice 00.

Slice 05 adds a service-local Dockerfile for
`services/repository-source-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist.

Slice 06 adds a service-local Dockerfile for `services/ingestion-service`.
This is target-service container packaging evidence only; Compose, Swarm and
Kubernetes readiness for the FA-MSA-001 target landscape remains future work
until descriptors and validation commands exist.

Slice 07 adds a service-local Dockerfile for
`services/java-parser-analysis-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist.

Slice 08 adds a service-local Dockerfile for `services/joern-analysis-service`.
This is target-service container packaging evidence only; Compose, Swarm and
Kubernetes readiness for the FA-MSA-001 target landscape remains future work
until descriptors and validation commands exist. The service uses local gRPC
port `9096` and health port `8087`, distinct from predecessor Joern and
JavaParser target ports. Docker image build or Joern runtime smoke testing is
optional external verification because it may pull the digest-pinned Joern base
image or create local container state.

Slice 09 adds a service-local Dockerfile for
`services/analysis-orchestrator-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist. The service uses local gRPC port `9098` and health
port `8089`, distinct from predecessor Analysis Store and earlier target
service ports.

Slice 15 later verified the local repository-to-BTM Compose descriptor with
six service `bootJar` tasks, `docker compose config`, `docker compose build`,
Compose startup, Gateway plus service health checks and Compose cleanup. This
is local repository-to-BTM readiness only and does not claim production-wide
Compose, Swarm or Kubernetes readiness.

The active E2E/WildFly/CLI workflow records a separate Swarm and Kubernetes
workflow handoff in `docs/workflow/deployment-workflow-request.md`. That handoff
does not add stack files, manifests or readiness claims.

Missing deployment material:

- `deployment/docker-compose/docker-compose.yml`
- `deployment/docker-swarm/stack.yml`
- `deployment/kubernetes/**`
- Helm or chart roots
- service-local Dockerfiles for README-only planned services
- verified service healthcheck definitions

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

Service-specific test commands exist for the seven registered service projects.
Graph-replay and report-generation still have no service-local Gradle test task
because they remain README-only planned service roots and are deferred from
repository-to-BTM acceptance. The build-artifact worker has no Gradle task
until a later slice creates the service root.

Slice 18 does not remove any `forensic-analytics-*` module from the Gradle
build. Those modules remain part of the current quality gate as legacy
in-process and rollback paths until a later slice proves caller removal,
replacement parity and rollback evidence.

Slice 19 also leaves the Gradle module list unchanged. Removal is blocked
until caller-verification searches prove that a module has no remaining
production or test caller and parity or explicit deprecation tests exist.

## Active E2E/WildFly/CLI Workflow Verification

Workflow `e2e-wildfly-cli-deploy-20260521-v1` adds current verification
evidence without changing the default build topology:

- a deterministic local real repository E2E fixture in
  `forensic-analytics-testbed`;
- an opt-in WildFly hardening scenario that is skipped by default unless an
  explicit external WildFly branch or commit is provided;
- an explicit CLI `gateway-submit` command for Gateway/public API submission;
- a caller inventory showing that local `analyze`, REST, Bootstrap, Boot,
  Engine, Ingestion Request and Testbed paths remain active legacy callers;
- a separate Swarm and Kubernetes deployment workflow request, with no stack
  files, manifests or readiness claims added by this workflow.

The workflow full local quality gate passed after the S07 CLI coverage
remediation:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Residual Risks

- CI workflow evidence is absent.
- Service-specific test tasks exist only for the seven registered service
  projects.
- Contract tests exist for implemented service contracts, but coverage is not
  complete for every planned service interaction.
- Frontend coverage and end-to-end test gates are not configured.
- Boot container healthcheck evidence is absent.
- Swarm and Kubernetes readiness is absent.
- WildFly hardening is optional external evidence and is not part of the
  default Gradle quality gate.
- Baseline documentation-only reviews still require only their documented
  `git diff --check` gate unless a later workflow slice changes product code,
  build logic, tests or deployment behavior.
