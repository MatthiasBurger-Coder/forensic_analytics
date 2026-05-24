# ADR-0022: Retire legacy modular-monolith source trees

## Status

Accepted

## Date

2026-05-23

## Context

ADR-0017 accepted the FA-MSA-001 target service landscape and allowed removal
of the central modular-monolith source trees only after caller-free evidence,
replacement tests, rollback notes and required quality gates existed.

Workflow `fa-msa-001-final-legacy-source-retirement-20260523-v2` completed
S05 with checkpoint commit `d8d9dab`. That slice physically removed the 16
tracked top-level legacy source trees:

- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-application`
- `forensic-analytics-boot-app`
- `forensic-analytics-bootstrap`
- `forensic-analytics-cli`
- `forensic-analytics-domain`
- `forensic-analytics-engine`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-logging`
- `forensic-analytics-observability`
- `forensic-analytics-persistence`
- `forensic-analytics-rest`
- `forensic-analytics-testbed`

S05 verification recorded:

- `git ls-files "forensic-analytics-*"` returned no tracked files;
- active non-legacy Gradle build files had no
  `project(":forensic-analytics-*")` dependency;
- active non-legacy Java source files had no imports from retired monolith
  packages;
- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`
  passed and listed only the `services` project hierarchy;
- `./gradlew test --dependency-verification strict --console=plain --stacktrace`
  passed.

## Decision

Retire the listed legacy modular-monolith source trees as implementation
source. The active Java build is now service-root based under `services:*`.

This decision closes the ADR-0017 condition that the central
`forensic-analytics-*` source trees remained tracked migration inputs. It does
not supersede ADR-0017's target service landscape, ADR-0009's no-shared-Java
rule, ADR-0010's contract-first rule, ADR-0013's data ownership rule or
ADR-0019's service-local Spring Boot boundary.

Do not reintroduce a shared Java domain, application, DTO, repository, service,
utility, fixture, logging, persistence or internal error-model module to
replace the retired source trees. Service communication remains limited to
REST/OpenAPI, gRPC/protobuf, approved message contracts or documented file
contracts.

Contract compatibility vocabulary may remain when explicitly documented as
predecessor compatibility or provenance. Such vocabulary is not evidence that
the retired implementation source trees are active.

Rollback for this source-tree retirement is to revert the S05 checkpoint
commit. The repository must not keep deleted source trees as current runtime,
build, test or deployment rollback units.

## Consequences

- Documentation may keep legacy module names only as dated historical evidence,
  predecessor provenance, contract compatibility vocabulary or ADR history.
- Architecture and workflow documents must describe the current build as
  service-only under `services:*`.
- No public contract shape, endpoint, RPC, event, runtime behavior, Docker image
  readiness, healthcheck readiness, Swarm readiness, Kubernetes readiness,
  durable persistence, graph/replay capability, report-generation capability or
  live LLM capability is accepted by this ADR.
- Persistence ownership gaps for workspace/project administration, membership,
  assets, audit, retention and legacy project-storage behavior remain explicit
  unresolved requirement topics. Deleting `forensic-analytics-persistence` does
  not assign those responsibilities to another service.
- S07 still owns the full local quality gate before final workflow release
  readiness.

## Related Documents

- `docs/workflow/workflow.md`
- `docs/workflow/execution-report.md`
- `docs/architecture/service-migration-map.md`
- `docs/architecture/service-boundaries.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/09-architecture-decisions.md`
