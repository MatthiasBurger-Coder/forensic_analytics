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

Verified from `settings.gradle.kts` after ADR-0022 and S05:

- `btm-generation-service`
- `joern-cpg-analysis-service`
- `joern-analysis-service`
- `analysis-orchestrator-service`
- `java-parser-analysis-service`
- `java-ast-analysis-service`
- `repository-source-service`
- `repository-analysis-service`
- `analysis-store-service`
- `ingestion-service`
- `forensic-ingestion-service`
- `query-report-api-service`
- `cli-client`
- `observability-stack`
- `testbed`
- `forensic-gateway-service`

Eighteen service-specific Gradle projects under top-level service roots
(`*-service/**`, `cli-client/**`, `observability-stack/**`, `testbed/**`) are registered.
No `forensic-analytics-*` Gradle project remains in the active model.
`repository-source-service`, `ingestion-service` and
`java-parser-analysis-service`, `joern-analysis-service` and
`analysis-orchestrator-service` and
`query-report-api-service` are FA-MSA-001 target-name backend service
projects introduced by this workflow. `cli-client` is a registered
FA-MSA-001 public API client boundary, not a productive backend service.
`observability-stack` is a registered deployment-oriented
observability boundary, not a productive backend service and not a shared Java
runtime module.
`testbed` is a registered non-production integration and system-test
boundary. It is not a productive backend service and must not be used as a
shared runtime dependency by production services.
`repository-analysis-service`, `forensic-ingestion-service`,
`java-ast-analysis-service` and `joern-cpg-analysis-service`
remain predecessor services and rollback inputs, not compatibility aliases.
Graph-replay and report-generation remain planned service roots with placeholder
build scripts and are explicitly deferred from repository-to-BTM acceptance by Slice 16. The
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

Verified Java test sources exist under service-local `src/test/java` trees.
The active test suite covers registered service projects and service-local
contract, architecture and regression scenarios. Former monolith module tests
are historical pre-retirement evidence after S05.

`testbed` includes the deterministic local real repository E2E
fixture under `src/test/resources/repository-e2e/`. The corresponding
service-root testbed coverage materializes that fixture as a local Git
repository, pins a commit, and verifies the retained regression surface without
restoring a shared monolith testbed module.

WildFly large-repository hardening is available as an opt-in service-root
testbed scenario through `WildFlyRepositoryHardeningTest`. It is skipped by
default unless
`FORENSIC_ANALYTICS_WILDFLY_HARDENING=true` and an explicit WildFly branch or
commit is provided. The runbook is
`docs/arc42/10-quality-requirements/testing/wildfly-hardening.md`.

Slice 13 verifies the same testbed coverage under `testbed` in package
`de.burger.forensics.analytics.services.testbed`, including the default-skipped
WildFly hardening scenario. The predecessor `forensic-analytics-testbed` source
tree is historical quality evidence; the repository root quality gate is
service-only and final retirement is governed by S05 deletion plus S06/S07
closure.

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

- `docker/boot-app/README.md` as historical Boot-app Docker documentation
- `docker/joern/Dockerfile`
- `docker/joern/docker-compose.joern.yml`
- `docker/joern/scripts/**`
- `forensic-ui/Dockerfile`
- `repository-source-service/Dockerfile`
- `ingestion-service/Dockerfile`
- `java-parser-analysis-service/Dockerfile`
- `joern-analysis-service/Dockerfile`
- `analysis-orchestrator-service/Dockerfile`
- `query-report-api-service/Dockerfile`
- `deployment/docker-compose/repository-to-btm.local.yml`

The DevOps review verified Joern Compose configuration with:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config
```

No Boot jar build, Boot Docker image build, frontend image build, Swarm command
or Kubernetes command was executed as part of Slice 00.

Earlier FA-MSA-001 service-slice evidence adds a service-local Dockerfile for
`repository-source-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist. FA-MVP-0001 S09 adds Docker-local Compose model
evidence for repository-source private workspace volume and PostgreSQL metadata configuration, but does
not claim image startup, health-check smoke testing, Swarm or Kubernetes
readiness.

Earlier FA-MSA-001 service-slice evidence adds a service-local Dockerfile for
`ingestion-service`.
This is target-service container packaging evidence only; Compose, Swarm and
Kubernetes readiness for the FA-MSA-001 target landscape remains future work
until descriptors and validation commands exist.

Earlier FA-MSA-001 service-slice evidence adds a service-local Dockerfile for
`java-parser-analysis-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist.

Earlier FA-MSA-001 service-slice evidence adds a service-local Dockerfile for
`joern-analysis-service`.
This is target-service container packaging evidence only; Compose, Swarm and
Kubernetes readiness for the FA-MSA-001 target landscape remains future work
until descriptors and validation commands exist. The service uses local gRPC
port `9096` and health port `8087`, distinct from predecessor Joern and
JavaParser target ports. The root `.dockerignore` allows the service boot jar
into the Docker build context. Docker image build or Joern runtime smoke
testing is optional external verification because it may pull the
digest-pinned Joern base image or create local container state.

Earlier FA-MSA-001 service-slice evidence keeps a service-local Dockerfile for
`analysis-orchestrator-service`. This is target-service container
packaging evidence only; Compose, Swarm and Kubernetes readiness for the
FA-MSA-001 target landscape remains future work until descriptors and
validation commands exist. The service uses local gRPC port `9098` and health
port `8089`, distinct from predecessor Analysis Store and earlier target
service ports. S07 does not claim Docker image build readiness; Docker
build-context verification remains later deployment work.

Earlier FA-MSA-001 service-slice evidence keeps a service-local Dockerfile for
`query-report-api-service`. This is target-service container packaging
evidence only; Compose, Swarm and Kubernetes readiness for the FA-MSA-001
target landscape remains future work until descriptors and validation commands
exist. The service uses local HTTP port `8080`, points at
`analysis-orchestrator-service` gRPC port `9098`, and remains a public facade
for verified repository-analysis routes plus pending/status-only orchestrator
readiness and the FA-MVP-0001 workspace routes backed by repository-source
owner APIs. S08/S11 do not claim Docker image build readiness; Docker
build-context verification remains later deployment work.

Slice 13 adds `testbed` without a Dockerfile or runtime service
descriptor. It may use verified local deployment descriptors as test
environment evidence, but S13 does not add Compose, Swarm or Kubernetes
readiness.

Slice 14 was a historical retirement-readiness documentation and evidence gate.
It did not remove any Gradle module, source tree or default runtime path because
caller scans at that time found build, production and test references to
retained `forensic-analytics-*` modules. ADR-0022 and the active
final-retirement workflow supersede that evidence with S04 documentation
cleanup, S05 deletion, S06 architecture closure and S07 release readiness.

Earlier Slice 15 relocated broad logging and Spring architecture checks from the
legacy-dependent `testbed` classpath into service-local architecture
tests for the productive target services. `testbed` keeps only the
default-skipped WildFly hardening scenario as historical targeted evidence.
That work does not verify Docker image builds, Docker Compose startup, service
health probes, Docker Swarm or Kubernetes commands for the FA-MSA-001 target
landscape.
The local repository-to-BTM Compose descriptor is verified as Docker-local
configuration evidence after S09. It remains transitional environment evidence
only until a separate runtime-deployment slice verifies image builds, startup,
health checks and cleanup commands.

No separate Swarm or Kubernetes workflow handoff artifact is present in this
repository. Future deployment workflows must create and verify
their own stack files, manifests, commands and readiness evidence.

Missing deployment material:

- `deployment/docker-compose/docker-compose.yml`
- `deployment/docker-swarm/stack.yml`
- `deployment/kubernetes/**`
- Helm or chart roots
- service-local Dockerfiles for planned services without runtime implementation
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

Service-specific test commands exist for the registered service projects under
top-level service roots (`*-service/**`, `cli-client/**`, `observability-stack/**`, `testbed/**`).
Graph-replay and report-generation have placeholder build scripts but no
implementation tests because they remain planned service roots and are deferred
from repository-to-BTM acceptance. The build-artifact worker has no Gradle task
until a later slice creates the service root.

Earlier Slice 18 did not remove any `forensic-analytics-*` source tree. That
prior evidence is superseded by the final-retirement workflow: the verified
Gradle project model is service-only as top-level service projects, S05 removed the legacy
source trees, and S06/S07 close architecture and release-readiness evidence.

## Historical E2E/WildFly/CLI Workflow Verification

Workflow `e2e-wildfly-cli-deploy-20260521-v1` added historical verification
evidence without changing the service-only target topology:

- a deterministic local real repository E2E fixture that later moved to
  service-root testbed evidence;
- an opt-in WildFly hardening scenario that is skipped by default unless an
  explicit external WildFly branch or commit is provided;
- an explicit CLI `gateway-submit` command for Gateway/public API submission;
- a predecessor caller inventory for local `analyze`, REST, Bootstrap, Boot,
  Engine, Ingestion Request and Testbed paths;
- a separate Swarm and Kubernetes deployment workflow request, with no stack
  files, manifests or readiness claims added by this workflow.

The full local quality gate remains a deletion and closure requirement for the
active S07 release-readiness slice:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Residual Risks

- CI workflow evidence is absent.
- Service-specific test tasks exist only for registered service projects under
  top-level service roots (`*-service/**`, `cli-client/**`, `observability-stack/**`, `testbed/**`).
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
