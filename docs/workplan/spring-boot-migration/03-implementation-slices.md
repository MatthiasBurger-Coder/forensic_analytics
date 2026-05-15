# Implementation Slices

Each slice begins with read-only verification of the files it will touch. Stop instead of guessing if any expected file, task, dependency, schema field, package or contract cannot be verified.

## Slice 00 - Inventory And Workplan

Purpose:

- Document the verified baseline for the Spring Boot migration.
- Replace the previous active `docs/workplan` with this workplan.

Prerequisites:

- User request.
- Root `AGENTS.md`.
- Root `QUALITY.md`.

Affected files:

- `docs/workplan/**`

Owner role:

- Senior Workplan Architect

Allowed write scope:

- Workplan documentation only.

Dependencies:

- none

Parallelization status:

- blocking

Done criteria:

- repository baseline is documented
- current module names are mapped to the requested target structure
- quality gate authority is documented
- stop conditions are explicit

Verification commands:

```bash
git status --short docs/workplan
```

Stop conditions:

- `docs/workplan` cannot be safely regenerated
- quality-gate authority is unclear
- current module ownership cannot be verified

## Slice 01 - Spring Boot Architecture Decision

Purpose:

- Add an ADR that accepts Spring Boot as an outer server/bootstrap technology.
- Clarify that ADR-0005 still forbids Spring AOP, AspectJ and SLF4J in the observability boundary unless a later ADR supersedes it.
- Decide whether current module names remain canonical for this migration.

Prerequisites:

- Slice 00 complete.

Affected files:

- `docs/adr/ADR-0006-spring-boot-server-boundary.md`
- `docs/arc42/02-architecture-constraints.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`

Owner role:

- Senior System Architect
- Senior Documentation Engineer

Allowed write scope:

- ADR and architecture documentation only.

Dependencies:

- Slice 00

Parallelization status:

- blocking for dependency and module changes

Done criteria:

- Spring Boot is explicitly scoped to outer modules
- domain/application independence remains documented
- current module naming policy is explicit
- observability policy is not contradicted

Verification commands:

```bash
git diff -- docs/adr docs/arc42
```

Stop conditions:

- Spring Boot adoption conflicts with accepted ADRs and no superseding decision is documented
- architecture docs would require guessing a storage, graph, LLM or UI technology

## Slice 02 - Version Catalog And Dependency Verification

Purpose:

- Add central Spring Boot `4.0.6` version aliases.
- Add only the first required Spring Boot dependencies.
- Update strict dependency verification metadata.

Prerequisites:

- Slice 01 complete.
- Gradle file hash cache lock is clear.

Affected files:

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `gradle/verification-metadata.xml`

Owner role:

- Senior DevOps
- Senior Java Backend

Allowed write scope:

- Gradle catalog, plugin declaration and dependency metadata only.

Dependencies:

- Slice 01

Parallelization status:

- blocking for Spring Boot implementation

Done criteria:

- Spring Boot plugin is declared with `apply false` at the root if added there
- strict dependency verification metadata contains only required new artifacts
- domain and application classpaths remain Spring-free
- Boot starter logging dependencies are absent unless a later ADR accepts them or verified exclusions remove them

Verification commands:

```bash
./gradlew --write-verification-metadata sha256 <task-that-resolves-the-failing-configuration> --console=plain --stacktrace
./gradlew :forensic-analytics-domain:dependencies --configuration compileClasspath --dependency-verification strict --console=plain
./gradlew :forensic-analytics-application:dependencies --configuration compileClasspath --dependency-verification strict --console=plain
```

Stop conditions:

- Spring dependencies cannot resolve
- dependency metadata includes unrelated artifacts
- Spring Boot starter logging dependencies appear without accepted ADR coverage or verified exclusions
- Spring appears on domain or application compile classpath
- Gradle task discovery remains blocked by a stale lock

## Slice 03 - Spring Boot App Module

Purpose:

- Add `forensic-analytics-boot-app` as the Spring Boot application module.
- Create the minimal boot entrypoint.
- Keep the existing manual `forensic-analytics-bootstrap` module intact.

Prerequisites:

- Slice 02 complete.

Affected files:

- `settings.gradle.kts`
- `forensic-analytics-boot-app/build.gradle.kts`
- `forensic-analytics-boot-app/src/main/java/de/burger/forensics/analytics/boot/ForensicAnalyticsApplication.java`
- `forensic-analytics-boot-app/src/main/resources/application.properties`
- `forensic-analytics-boot-app/src/test/java/**`

Owner role:

- Senior Java Backend
- Senior DevOps
- Senior Tester

Allowed write scope:

- New boot app module and settings inclusion.

Dependencies:

- Slice 02

Parallelization status:

- blocking for Spring configuration and lifecycle slices

Done criteria:

- application starts with an empty or minimal Spring context
- profiles `local`, `test`, `docker` and `prod` are represented without unsafe defaults
- no domain/application package contains Spring annotations
- smoke test verifies Spring context startup

Verification commands:

```bash
./gradlew :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-application:test :forensic-analytics-ingestion-grpc:test :forensic-analytics-rest:test :forensic-analytics-observability:test :forensic-analytics-testbed:test --tests '*ArchitectureTest' --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Spring component scanning pulls domain or application classes into framework annotations
- application startup requires external services
- profile defaults write outside configured workspace paths

## Slice 04 - Typed Spring Configuration

Purpose:

- Introduce typed configuration properties for workspace, gRPC, REST, Joern and observability settings.
- Preserve current property intent from manual bootstrap.

Prerequisites:

- Slice 03 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/config/**`
- `forensic-analytics-boot-app/src/main/resources/application.properties`
- `forensic-analytics-boot-app/src/test/java/**`
- optional `docs/arc42/07-deployment-view.md`

Owner role:

- Senior Java Backend
- Senior DevOps

Allowed write scope:

- Boot app configuration classes, resources and matching tests.

Dependencies:

- Slice 03

Parallelization status:

- can run in parallel with Slice 09 after Slice 03 if write scopes stay disjoint

Done criteria:

- gRPC defaults match verified current behavior: enabled by default, port `9090`
- REST defaults match verified current behavior: enabled by default, host `127.0.0.1`, port `8080`
- workspace base path is explicit
- Joern is disabled by default unless a verified environment enables it
- invalid ports and blank paths fail fast

Verification commands:

```bash
./gradlew :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- property names conflict with existing documented keys without migration notes
- configuration requires unverified storage or container dependencies

## Slice 05 - Spring Bean Wiring For Existing Use Cases

Purpose:

- Recreate the manual wiring behavior from `ForensicAnalyticsBackendComponents` as Spring-managed bean configuration inside the boot app.
- Reuse existing application use cases and adapters.

Prerequisites:

- Slice 04 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/config/**`
- optional package-private visibility changes only if verified and tested
- `forensic-analytics-boot-app/src/test/java/**`

Owner role:

- Senior Java Backend
- Senior Tester

Allowed write scope:

- Boot app bean configuration and tests.
- Minimal visibility changes in existing modules only when exact target symbols are verified.

Dependencies:

- Slice 04

Parallelization status:

- blocking for gRPC and REST Spring lifecycle integration

Done criteria:

- Spring creates beans for existing ingestion and query use cases
- existing in-memory repositories can be wired for local/test profiles
- no application use case gains Spring annotations
- manual bootstrap still compiles and runs
- `ForensicAnalyticsBackendComponents` is treated as a verified wiring inventory, not as a directly reused API, unless a dedicated extraction slice changes its package-private boundary

Verification commands:

```bash
./gradlew :forensic-analytics-boot-app:test :forensic-analytics-bootstrap:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- existing constructors or factories are package-private and cannot be reused without unsafe visibility changes
- public constructors or ports needed for wiring cannot be verified
- Spring wiring would require moving business logic into the boot module

## Slice 06 - gRPC Server Lifecycle In Spring Boot

Purpose:

- Start the existing gRPC ingestion service from the Spring Boot app.
- Keep the proto contract unchanged.
- Keep analysis logic outside the gRPC adapter.

Prerequisites:

- Slice 05 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/grpc/**`
- `forensic-analytics-boot-app/src/test/java/**`
- possibly `forensic-analytics-ingestion-grpc/src/main/java/**` only for verified constructor or lifecycle support
- `forensic-analytics-ingestion-grpc/src/test/java/**`

Owner role:

- Senior gRPC/Protobuf Specialist
- Senior Java Backend
- Senior Tester

Allowed write scope:

- Boot lifecycle wrapper for the existing gRPC service.
- Narrow gRPC adapter changes only when required by verified constructor or lifecycle contracts.

Dependencies:

- Slice 05

Parallelization status:

- can run in parallel with Slice 07 after Slice 05 if REST files are not touched

Done criteria:

- Boot app starts gRPC on configured port
- disabled gRPC profile/property prevents server startup
- gRPC status mapping remains unchanged
- `UploadAnalysisData` stream behavior remains unchanged
- no proto field is repurposed as an operational correlation ID

Verification commands:

```bash
./gradlew :forensic-analytics-boot-app:test :forensic-analytics-ingestion-grpc:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- gRPC Spring lifecycle requires an unverified third-party starter
- port binding tests are nondeterministic
- proto changes become necessary
- application/domain dependency on gRPC appears

## Slice 07 - REST/API Spring Strategy

Purpose:

- Decide whether Spring Boot initially wraps the existing JDK REST adapter, runs without REST, or introduces Spring MVC/WebFlux in a dedicated API module.

Prerequisites:

- Slice 05 complete.

Affected files:

- `docs/adr/ADR-0007-rest-api-spring-strategy.md`
- `forensic-analytics-boot-app/src/main/java/**`
- optional `forensic-analytics-rest/**` only after a decision

Owner role:

- Senior System Architect
- Senior Java Backend
- Senior Tester

Allowed write scope:

- ADR and selected REST integration files only.

Dependencies:

- Slice 05

Parallelization status:

- can run in parallel with Slice 06 after Slice 05

Done criteria:

- REST startup behavior is explicit
- existing API contracts are preserved or documented as intentionally migrated
- browser/API DTOs are not reused from gRPC transport classes
- runtime data sensitivity, redaction and authorization boundaries are documented before exposing any new runtime-evidence endpoint
- tests cover enabled and disabled REST behavior

Verification commands:

```bash
./gradlew :forensic-analytics-rest:test :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Spring MVC/WebFlux dependency choice is unclear
- REST contract changes are required but not documented
- REST starts before authorization and evidence-sensitivity boundaries are verified
- new REST endpoints would expose runtime values, stack traces, LLM prompts or source content before redaction and audit rules are verified

## Slice 08 - Observability Bridge Review

Purpose:

- Preserve current sanitized operation logging.
- Decide whether Spring Boot needs only bean wiring or a dedicated Spring logging bridge.
- Keep `logging.zip` AOP/MDC behavior out of core modules unless a new ADR accepts it.

Prerequisites:

- Slice 03 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/observability/**`
- `forensic-analytics-observability/**` only for framework-neutral changes
- `forensic-analytics-testbed/src/test/java/**/LoggingArchitectureTest.java`
- optional ADR if Spring-specific logging is accepted

Owner role:

- Senior System Architect
- Senior Security Sandbox Engineer
- Senior Java Backend
- Senior Tester

Allowed write scope:

- Boot observability bean wiring.
- Architecture tests.
- Framework-neutral observability adjustments only when verified.

Dependencies:

- Slice 03

Parallelization status:

- can run after Slice 03; blocks broad adapter logging changes

Done criteria:

- Spring Boot startup logs do not expose raw evidence or local secrets
- domain/application still do not depend on observability
- observability still does not depend on Spring, SLF4J, AspectJ, gRPC or REST unless a new ADR explicitly changes the rule
- `logging.zip` is documented as source material, not copied blindly

Verification commands:

```bash
./gradlew :forensic-analytics-observability:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-testbed:test --tests '*LoggingArchitectureTest' --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- method argument/result logging is required
- MDC propagation across async boundaries is required but unmodeled
- logging dependencies leak into domain/application

## Slice 09 - Constructor Explicitness And Dependency Minimization

Purpose:

- Keep Spring Boot wiring classes explicit enough to avoid adding Lombok or annotation-processing dependencies.
- Prove the Boot module compiles without generated constructor behavior.

Prerequisites:

- Slice 02 complete.

Affected files:

- Boot lifecycle/configuration classes only when constructor boilerplate appears
- selected module `build.gradle.kts` files
- version catalog and dependency verification metadata if a candidate dependency is removed

Owner role:

- Senior Java Backend
- Senior Tester

Allowed write scope:

- Explicit constructors in Boot adapter classes.
- Removal of unused Lombok catalog, metadata or build entries if introduced during the migration.

Dependencies:

- Slice 02

Parallelization status:

- can run in parallel with Slice 04 if files are disjoint

Done criteria:

- no Lombok dependency alias or annotation processor is needed for the Boot module
- constructors remain explicit in lifecycle classes
- compile and tests pass under Java 25

Verification commands:

```bash
./gradlew :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
rg -n "lombok|RequiredArgsConstructor" forensic-analytics-boot-app gradle/libs.versions.toml gradle/verification-metadata.xml
```

Stop conditions:

- a Spring Boot class requires generated methods to remain understandable
- removing Lombok would require unrelated source changes
- dependency verification still contains unused Lombok metadata after removal

## Slice 10 - Persistence And Workspace Configuration

Purpose:

- Prepare Spring-configurable persistence and workspace adapters without selecting a production database prematurely.

Prerequisites:

- Slice 05 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/config/**`
- `forensic-analytics-persistence/**`
- `forensic-analytics-adapter-repository-source/**`
- related tests
- optional ADR for initial relational database choice

Owner role:

- Senior Analysis Storage Architect
- Senior Security Sandbox Engineer
- Senior Java Backend

Allowed write scope:

- Configuration and adapter wiring.
- Storage implementation changes only after exact ports and contracts are verified.

Dependencies:

- Slice 05

Parallelization status:

- can run after Slice 05; should not overlap with gRPC files

Done criteria:

- workspace base path is profile-specific and validated
- path traversal protections remain intact
- in-memory storage remains available for tests
- H2/PostgreSQL is not added until a database ADR exists

Verification commands:

```bash
./gradlew :forensic-analytics-persistence:test :forensic-analytics-adapter-repository-source:test :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- storage schema or database product is unclear
- workspace paths could escape configured roots
- persistence adapters leak into domain/application

## Slice 11 - Joern And External Tool Configuration

Purpose:

- Make Joern adapter settings Spring-configurable while preserving optional execution.

Prerequisites:

- Slice 04 complete.

Affected files:

- `forensic-analytics-boot-app/src/main/java/**/config/**`
- `forensic-analytics-adapter-joern-docker/**`
- `docker/joern/docker-compose.joern.yml`
- related tests and docs

Owner role:

- Senior Joern CPG Specialist
- Senior DevOps
- Senior Security Sandbox Engineer

Allowed write scope:

- Joern configuration, adapter wiring and tests.

Dependencies:

- Slice 04

Parallelization status:

- can run after Slice 04 if it does not modify shared boot configuration files concurrently

Done criteria:

- Joern is disabled by default unless profile enables it
- image name, timeout and workspace mount behavior are explicit
- failures are represented as adapter failures, not fabricated semantic evidence
- tests do not require a live Joern container by default

Verification commands:

```bash
./gradlew :forensic-analytics-adapter-joern-docker:test :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Docker is required for unit tests
- Joern output shape is assumed without fixture verification
- container mounts can access uncontrolled host paths

## Slice 12 - Docker And Deployment Baseline

Purpose:

- Add container startup for the Spring Boot server.
- Keep Joern and database containers optional until their slices select concrete runtime dependencies.

Prerequisites:

- Slices 03, 06 and 10 complete.

Affected files:

- `Dockerfile` or `docker/**`
- `compose.yaml` or `docker/**`
- `docs/arc42/07-deployment-view.md`
- `docs/README.md`
- boot app build files if packaging changes are needed

Owner role:

- Senior DevOps
- Senior Security Sandbox Engineer

Allowed write scope:

- Container and deployment documentation.
- Boot app packaging configuration.

Dependencies:

- Slice 03
- Slice 06
- Slice 10

Parallelization status:

- late integration slice

Done criteria:

- container exposes configured gRPC port
- workspace volume is explicit
- healthcheck is documented and implemented only if the required endpoint exists
- image does not include local secrets or generated evidence

Verification commands:

First verify that the Spring Boot plugin has introduced the `bootJar` task for the boot app:

```bash
./gradlew :forensic-analytics-boot-app:tasks --all --dependency-verification strict --console=plain --stacktrace
```

Only after `bootJar` is verified:

```bash
./gradlew :forensic-analytics-boot-app:bootJar --dependency-verification strict --console=plain --stacktrace
```

Optional after Docker files exist:

```bash
docker compose config
```

Stop conditions:

- container relies on unavailable health endpoints
- Docker build copies uncontrolled workspace data
- database or Joern service ownership is unclear

## Slice 13 - Documentation And Architecture Test Synchronization

Purpose:

- Synchronize README, arc42, ADRs and architecture tests with the implemented Spring Boot boundary.

Prerequisites:

- All implementation slices whose behavior is documented.

Affected files:

- `docs/README.md`
- `docs/arc42/**`
- `docs/adr/**`
- architecture tests under `src/test/java/**/quality` or testbed packages

Owner role:

- Senior Documentation Engineer
- Senior System Architect
- Senior Tester

Allowed write scope:

- Documentation and architecture tests tied to implemented behavior.

Dependencies:

- Slices 01 through 12 as applicable

Parallelization status:

- can run alongside implementation review but final update must follow actual code

Done criteria:

- start commands, profiles, ports and module boundaries are documented
- accepted ADRs do not contradict implementation
- architecture tests protect Spring-free domain/application rules
- documentation does not mention unimplemented dependencies as active behavior

Verification commands:

```bash
./gradlew :forensic-analytics-application:test :forensic-analytics-ingestion-grpc:test :forensic-analytics-rest:test :forensic-analytics-observability:test :forensic-analytics-testbed:test --tests '*ArchitectureTest' --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- docs require claiming a feature that is not implemented
- architecture tests must be weakened rather than refined

## Slice 14 - Final Quality Gate

Purpose:

- Run final repository verification and record results.

Prerequisites:

- All selected implementation slices complete.

Affected files:

- `docs/workplan/spring-boot-migration/quality-log.md`

Owner role:

- Senior Tester
- Senior DevOps

Allowed write scope:

- Quality log updates only, unless failures require separate repair slices.

Dependencies:

- all selected implementation slices

Parallelization status:

- final blocking slice

Done criteria:

- minimum quality command passes
- full local quality gate passes
- Spring Boot specific tests pass
- skipped optional checks are documented with reasons

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Additional command when plugin metadata, Gradle task inputs, task outputs or plugin implementation classes change:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

Stop conditions:

- quality gate failure cause cannot be classified
- failures appear unrelated and require user decision before repair
- dependency verification fails due unreviewed metadata changes
