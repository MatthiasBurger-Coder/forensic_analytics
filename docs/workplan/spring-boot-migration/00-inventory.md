# Slice 00 Inventory

## Verification Date

2026-05-15

## Inspected Files And Sources

Read-only verification covered:

- `AGENTS.md`
- `QUALITY.md`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- existing `docs/workplan/`
- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md`
- `docs/arc42/02-architecture-constraints.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/adr/ADR-0001-plugins-are-producers.md`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`
- `docs/adr/ADR-0005-adapter-logging-observability-boundary.md`
- `.agents/roles/`
- `.agents/orchestrator/routing-rules.md`
- representative bootstrap, gRPC, REST, observability and architecture test files

External version availability was checked from public sources:

- Spring Boot `4.0.6` is published in the official Spring release announcement: <https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now>
- Spring Boot `4.0.6` `spring-boot-starter-logging` metadata is published on Maven Central: <https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-logging/4.0.6>

## Current Repository Baseline

| Area | Verified state |
|---|---|
| Java | Root Gradle build sets Java toolchain, source and target baseline to `25`. |
| Gradle | Wrapper uses `gradle-9.4.0-bin.zip`. |
| Test runtime | Version catalog defines JUnit `6.0.3`; root build uses JUnit platform. |
| Quality | `QUALITY.md` defines strict dependency verification and coverage gates. |
| Spring | No Spring dependency alias or Spring Boot plugin is currently configured. |
| Lombok | No Lombok dependency alias or annotation processor is currently configured; the migration keeps constructors explicit. |
| gRPC | Version catalog defines gRPC `1.80.0`; `forensic-analytics-ingestion-grpc` exists. |
| Protobuf | Version catalog defines Protobuf `4.34.1` and protobuf Gradle plugin `0.10.0`. |
| Logging | Current repository observability uses JDK `System.Logger`, not SLF4J, MDC, Spring AOP or AspectJ. |
| Dependency verification | `gradle/verification-metadata.xml` exists and must be updated for new dependencies. |

Verified Spring Boot starter logging impact:

- `spring-boot-starter-logging` `4.0.6` brings concrete logging dependencies including Logback and SLF4J bridges.
- ADR-0006 therefore must not treat the default `spring-boot-starter` path as logging-neutral without exclusions, dependency-tree verification or a later observability ADR.

## Current Included Modules

`settings.gradle.kts` includes:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
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

The repository also contains `forensic-ui`, but it is not included in `settings.gradle.kts`.

## Current Module Responsibility Map

| Existing module | Current responsibility | Spring Boot migration interpretation |
|---|---|---|
| `forensic-analytics-domain` | Domain model and value objects. | Must remain Spring-free and logging-framework-free. |
| `forensic-analytics-application` | Use cases, commands, ports and application services. | Must remain independent from Spring, gRPC, REST, persistence implementations and observability. |
| `forensic-analytics-engine` | Analysis execution support. | Adapter/application-adjacent implementation; wire through Spring only from outer modules. |
| `forensic-analytics-observability` | Correlation context and sanitized operation logging. | Keep as framework-neutral infrastructure unless a new ADR accepts a Spring-specific bridge. |
| `forensic-analytics-adapter-repository-source` | Workspace preparation and Git checkout adapters. | Spring-configurable outbound adapter. |
| `forensic-analytics-adapter-javaparser` | JavaParser source scanner adapter. | Spring-configurable outbound adapter. |
| `forensic-analytics-adapter-joern-docker` | Joern Docker adapter. | Spring-configurable outbound adapter with container and timeout properties. |
| `forensic-analytics-persistence` | In-memory persistence adapters today. | First persistence boundary for Spring bean wiring; H2/PostgreSQL require dedicated storage slices. |
| `forensic-analytics-ingestion-grpc` | Protobuf/gRPC contract, generated code and inbound service adapter. | Initial gRPC server adapter; split API/server modules only after an explicit contract slice. |
| `forensic-analytics-ingestion-request` | Engine request import. | Ingestion adapter/helper module. |
| `forensic-analytics-rest` | JDK `HttpServer` REST adapter. | Candidate for later Spring MVC/WebFlux replacement or coexistence behind the boot app. |
| `forensic-analytics-cli` | CLI entrypoint. | Keep Spring-free unless a CLI-specific decision accepts Spring Shell or Boot runner usage. |
| `forensic-analytics-bootstrap` | Manual server bootstrap for gRPC and REST. | Migration source for bean wiring and lifecycle behavior. |
| `forensic-analytics-testbed` | Cross-module tests and architecture checks. | Extend with Spring Boot architecture and smoke tests. |

No Maven adapter module is currently included in `settings.gradle.kts`. Maven support is an EPIC guardrail, but this Spring Boot migration workplan does not implement a Maven plugin adapter. If a Spring Boot slice requires Maven adapter behavior, stop and create a separate adapter workplan or ADR-backed slice.

## Current gRPC Contract

`forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto` defines `ForensicIngestionService` with:

- `AnalyzeRepository`
- `StartAnalysisSession`
- `UploadAnalysisData`
- `CompleteAnalysisSession`
- `AbortAnalysisSession`

The proto contains request, session, build and payload identity fields. It does not define a dedicated generic operational correlation ID field. Do not repurpose `request_id`, `session_id`, `build_id` or `project_id` as a generic logging correlation ID without a separate contract decision.

## Current Observability And logging.zip

The current repository observability module contains:

- `CorrelationContext`
- `CorrelationId`
- `CorrelationScope`
- `OperationLogger`
- `SystemOperationLogger`
- supporting operation log event and level classes

The user-referenced `logging.zip` exists at:

```text
/mnt/d/Projects/SCXMLExample/src/main/java/de/burger/it/scxmlexample/infrastructure/logging.zip
```

The ZIP contains:

- `CentralLoggingAspect.java`
- `Loggable.java`
- `LevelLogger.java`
- `LevelLoggerRegistry.java`
- `correlation/CorrelationIdManager.java`
- TRACE, DEBUG, INFO, WARN and ERROR strategy classes

Verified dependency and behavior impact:

- `CentralLoggingAspect` depends on Spring AOP, AspectJ, SLF4J and Spring component scanning.
- `CorrelationIdManager` depends on SLF4J MDC.
- Level strategies log method arguments, return values and exceptions.
- Those behaviors conflict with current ADR-0005 unless a new ADR supersedes the decision for Spring Boot specific infrastructure.
- Domain and application must not depend on `org.springframework`, `org.aspectj`, `org.slf4j`, MDC, concrete logging providers or the current observability module.

## Current Architecture Guardrails

Verified tests already forbid several unsafe dependencies:

- `AnalysisContractArchitectureTest` forbids application analysis contracts from depending on persistence, gRPC, Spring and selected providers.
- `IngestionGrpcArchitectureTest` forbids domain/application dependencies on gRPC/proto/observability and forbids gRPC adapter dependency on persistence.
- `LoggingArchitectureTest` forbids domain/application dependency on observability, SLF4J, Logback, Log4j, Spring, AspectJ, Micrometer and OpenTelemetry.
- `RestArchitectureTest` forbids application/domain dependency on REST infrastructure and observability.

Spring Boot adoption must update or extend these tests without weakening the core rule: Spring belongs outside the domain and application layers.

## Gradle Task Verification

`build.gradle.kts` defines `checkPackageCoverage`.

An attempt to run task discovery with:

```bash
./gradlew tasks --all --console=plain
```

was blocked by an existing Gradle file hash cache lock:

```text
Timeout waiting to lock file hash cache (/mnt/d/Projects/forensic_analytics/.gradle/9.4.0/fileHashes)
```

This workplan therefore documents only tasks verified from repository files or `QUALITY.md`. Future implementation must re-run task discovery after the lock is clear.

## Inventory Outcome

The migration is not a greenfield module layout or a complete EPIC delivery plan. It is a prerequisite server-bootstrapping migration over an already modular hexagonal codebase.

The EPIC MVP still includes separate capabilities for static import, canonical persistence, Joern attachment, Byteman generation, JSONL runtime import, incident creation, correlation replay, graph projection and LLM explanation. Those capabilities remain separate implementation work unless an individual slice explicitly verifies and owns them.

The safest starting point is:

1. keep current `forensic-analytics-*` module names,
2. add a new Spring Boot outer module only after an ADR/dependency slice,
3. wire existing application use cases and adapters as Spring beans from the outside,
4. keep existing manual bootstrap until Spring Boot parity is proven,
5. split gRPC contract/server modules only after contract and dependency verification.
