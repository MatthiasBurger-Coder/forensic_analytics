# Target Architecture

## Goal

Migrate Forensic Analytics toward a Spring Boot `4.0.6` based server application while keeping the forensic core framework-free, deterministic and evidence-first.

This workplan is a prerequisite infrastructure migration, not the full EPIC delivery plan. The EPIC MVP capabilities for runtime replay, JSONL runtime import, graph projection and LLM explanation remain separate implementation work unless a later slice explicitly owns them.

The build-tool plugin remains a producer-side adapter. It sends repository, build and scan data to the server through verified ingestion contracts. Runtime event ingestion remains governed by the EPIC's JSONL-first direction and open ingestion-mode decision; do not route generic runtime events through gRPC without a dedicated contract slice. The server owns processing, persistence, Joern enrichment, logging, replay, graph projection, LLM context preparation and future UI/API integration.

## Non-Goals

This workplan does not authorize:

- renaming every existing module to the shorter `analytics-*` names in one step
- moving domain or application code into Spring packages
- adding Spring AOP, AspectJ, SLF4J, Logback, Micrometer or OpenTelemetry to core modules
- changing public gRPC schema fields without a dedicated contract slice
- treating operational logs as forensic evidence
- replacing the current REST adapter before Spring Boot startup and gRPC parity are proven
- adding H2, PostgreSQL, graph DB, vector DB, LLM SDK or Docker dependencies without their own verified dependency slices
- implementing the missing Maven plugin adapter required by the EPIC guardrails
- changing runtime event ingestion from JSONL-first/open-decision status into gRPC transport without a dedicated contract decision
- weakening ArchUnit, coverage or dependency verification rules

## Requested Target Structure Mapped To Verified Modules

| Requested target | Verified repository mapping | Migration decision |
|---|---|---|
| `analytics-domain` | `forensic-analytics-domain` | Keep current module name. |
| `analytics-application` | `forensic-analytics-application` | Keep current module name. |
| `analytics-infrastructure` | existing persistence, source, JavaParser and Joern adapter modules | Do not collapse existing adapters into one broad module unless a later ADR requires it. |
| `analytics-observability` | `forensic-analytics-observability` | Keep current framework-neutral module; add Spring bridge only if approved. |
| `analytics-grpc-api` | currently part of `forensic-analytics-ingestion-grpc` | Optional later split after proto/package verification. |
| `analytics-grpc-server` | currently part of `forensic-analytics-ingestion-grpc` | Initially wire existing service through Spring lifecycle. |
| `analytics-cli` | `forensic-analytics-cli` | Keep Spring-free by default. |
| Maven plugin adapter | no verified Gradle module exists | Deferred to a separate adapter workplan; do not infer a Maven module during this migration. |
| `analytics-boot-app` | new `forensic-analytics-boot-app` | Add as the Spring Boot application module. |

## Target Dependency Direction

```text
forensic-analytics-boot-app
  -> inbound adapters and outbound adapters
  -> forensic-analytics-application
  -> forensic-analytics-domain
```

Examples:

```text
forensic-analytics-boot-app
  -> forensic-analytics-ingestion-grpc
  -> forensic-analytics-application
  -> forensic-analytics-domain
```

```text
forensic-analytics-boot-app
  -> forensic-analytics-adapter-joern-docker
  -> forensic-analytics-application
  -> forensic-analytics-domain
```

Forbidden:

```text
forensic-analytics-domain -> org.springframework
forensic-analytics-application -> org.springframework
forensic-analytics-application -> io.grpc
forensic-analytics-observability -> org.springframework
forensic-analytics-observability -> org.slf4j
```

## Boot App Responsibility

`forensic-analytics-boot-app` should own:

- `@SpringBootApplication`
- Spring bean wiring for use cases and adapters
- configuration properties
- profile-specific configuration
- server lifecycle
- gRPC server lifecycle integration
- REST/API server integration when selected
- health/readiness endpoints if Actuator is explicitly accepted
- Docker/container entrypoint behavior

It must not own:

- domain rules
- evidence normalization semantics
- parser behavior
- replay behavior
- graph schema decisions
- LLM prompt semantics
- persistence schema rules beyond configuration

## Observability Direction

The repository already has a JDK-based observability boundary. Spring Boot migration should start by reusing that boundary.

Use this default:

```text
Boot/adapters/bootstrap -> forensic-analytics-observability -> JDK System.Logger
```

Do not introduce annotation-driven method logging by default.

If Spring Boot logging, MDC, SLF4J or AOP is desired later, create a dedicated ADR that states:

- which module owns it,
- why JDK observability is insufficient,
- how raw evidence and secrets are excluded,
- how async context propagation is handled,
- how dependency verification and architecture tests are updated.

## LoggingPort Clarification

Operational logging is an infrastructure concern, not a domain decision. Do not add a domain-level `LoggingPort` only to let domain services write logs.

If a business-level audit, evidence note or diagnostic event is needed, define a typed application or domain model for that fact and name it according to its forensic meaning. Keep operational logging separate from forensic evidence.

## gRPC Direction

The current `forensic-analytics-ingestion-grpc` module contains both generated proto classes and the inbound service adapter. The first Spring Boot slice should wire the existing service as a bean and manage a gRPC `Server` lifecycle from the boot app.

Do not add an unverified third-party gRPC Spring Boot starter as a shortcut. Adopt an official or project-approved gRPC integration only after dependency, lifecycle, Java 25, Gradle 9.4.0 and strict verification impacts are documented.

## Configuration Direction

Spring Boot configuration should use explicit typed properties under the existing naming intent:

```properties
forensics.analytics.workspace.root-path=/var/lib/forensic-analytics
forensics.analytics.workspace.base-path=/var/lib/forensic-analytics/workspaces
forensics.analytics.workspace.allow-relative-paths=false
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.host=127.0.0.1
forensics.analytics.ingestion.grpc.port=9090
forensics.analytics.rest.enabled=true
forensics.analytics.rest.host=127.0.0.1
forensics.analytics.rest.port=8080
forensics.analytics.joern.enabled=false
forensics.analytics.joern.container-image=ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295
forensics.analytics.joern.output-directory=/var/lib/forensic-analytics/workspaces/joern
forensics.analytics.joern.timeout=PT5M
forensics.analytics.joern.fail-on-error=true
forensics.analytics.observability.logging.enabled=true
```

Use `forensics.analytics.*` unless a dedicated configuration ADR changes the prefix. The current manual bootstrap already uses `forensics.analytics.ingestion.grpc.*` and `forensics.analytics.rest.*` system properties.

## Target Acceptance Criteria

- Spring Boot `4.0.6` starts through `forensic-analytics-boot-app`.
- No Lombok or annotation-processor dependency is needed for Boot wiring classes.
- Domain and application stay Spring-free.
- Existing gRPC ingestion can be started by the Boot app.
- The plugin remains independent from Spring Boot.
- Runtime event ingestion mode remains JSONL-first/open unless a dedicated contract slice changes it.
- Maven adapter support is either preserved as a documented future adapter work item or implemented through a separate verified slice.
- Existing manual bootstrap is either preserved or intentionally retired after parity tests.
- `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace` passes.
- `docs/README.md`, arc42 and ADRs describe start commands, profiles, gRPC port and architecture boundaries.
