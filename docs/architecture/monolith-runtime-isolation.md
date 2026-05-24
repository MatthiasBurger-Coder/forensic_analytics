# Monolith Runtime Isolation

## Status

Earlier Slice 18 recorded the isolation decision for remaining
`forensic-analytics-*` runtime paths after the repository-to-BTM Gateway path
and frontend Gateway integration were implemented.

No `forensic-analytics-*` Gradle module was retired in Slice 18. That caller
verification was prior evidence; the active final-retirement workflow supersedes
it with current service-only project-model checks, S05 deletion, S06
architecture closure and S07 release readiness.

FA-MSA-001 Slice 14 reached the same historical retirement-readiness decision:
direct legacy module removal was `NO_REMOVAL_SAFE` until path-specific
migration, explicit deprecation, caller-free scans and the full quality gate
were available. Later service-root work deprecates the testbed runtime scenario
for target behavior rather than claiming full in-process
analysis parity.

S17 extends that decision to the former mini and real repository E2E tests.
`AnalyzeRepository`, local or file repository checkout and monolith
analysis-session registration remain legacy rollback evidence only. Target
coverage is split across `repository-source-service` repository preparation,
workspace cleanup and source snapshot tests, `ingestion-service`
`AnalyzeRepository` `UNIMPLEMENTED` tests and `analysis-orchestrator-service`
pending `StartRepositoryToBtm` status tests.

Earlier public API work moved the executable OpenAPI contract-test ownership
for the public repository-to-BTM submission/status shape into
`query-report-api-service` and added target CLI HTTP client coverage. This
removes the "OpenAPI contract tests remain owned only by
`forensic-analytics-rest`" blocker, but it does not make the retained runtime
modules caller-free or removal-safe.

## Isolation Decision

The accepted repository-to-BTM path now runs through:

```text
forensic-ui
  -> forensic-gateway-service
  -> analysis-store-service
  -> repository-analysis-service
  -> java-ast-analysis-service
  -> joern-cpg-analysis-service when inputs are complete
  -> btm-generation-service
```

The following former monolith runtime paths are retired source trees after S05.
They remain only as historical predecessor evidence and as rollback-by-revert
context under ADR-0022:

| Path | Historical role | Post-S05 decision |
|---|---|---|
| `forensic-analytics-cli` | Local in-process command adapter for analysis and engine-request import | Source tree retired by S05; `services:cli-client` is the active client boundary |
| `forensic-analytics-rest` | Predecessor JDK HTTP adapter used by Boot and bootstrap paths | Source tree retired by S05; public API contract-test ownership exists in `query-report-api-service` |
| `forensic-analytics-bootstrap` | Predecessor combined gRPC and REST runtime assembly | Source tree retired by S05; no active combined monolith runtime source remains |
| `forensic-analytics-boot-app` | Predecessor Spring Boot wrapper for monolith adapters | Source tree retired by S05; no active monolith Boot runtime source remains |
| `forensic-analytics-engine` | In-process facade around application repository analysis use cases | Source tree retired by S05; `analysis-orchestrator-service` owns the active target orchestration boundary |
| `forensic-analytics-ingestion-request` | JSON engine-request importer used by CLI | Source tree retired by S05; future import behavior requires service-owned contracts |
| `forensic-analytics-testbed` | In-process integration and architecture verification | Source tree retired by S05; active non-production evidence is under `services:testbed` |

These historical paths must not be described as implemented microservices or
as current runtime rollback units. Their behavior can be restored only by an
explicit rollback of the S05 checkpoint or by a new service-owned requirement.

## Retirement Preconditions

ADR-0022 records the final source-tree retirement. A later slice that restores,
reimplements or changes one of these behaviors must record:

- the verified replacement service owner;
- the exact public REST, gRPC or event contract;
- caller searches proving no remaining production or test dependency still
  needs the old path;
- parity or explicit deprecation tests;
- rollback or migration instructions that do not silently reintroduce shared
  Java implementation modules.

## Slice 19 Removal Review

Slice 19 did not remove shared implementation modules. That historical caller
review found module registrations and dependencies in `settings.gradle.kts`,
`forensic-analytics-testbed`, `forensic-analytics-boot-app`,
`forensic-analytics-bootstrap`, `forensic-analytics-cli`,
`forensic-analytics-rest`, `forensic-analytics-engine` and
`forensic-analytics-ingestion-request`. The active final-retirement workflow
supersedes that evidence with service-only project-model checks and S05
deletion readiness.

Because no module was both replaced and caller-free in that historical review,
deleting source roots or
editing Gradle registrations would violate the workflow stop condition for
Slice 19.

## Rollback

Rollback for the historical Slice 18 isolation decision is documentation-only.
Rollback for the actual S05 source-tree retirement is defined by ADR-0022:
revert checkpoint commit `d8d9dab` on the workflow branch and then rerun the
S05/S06/S07 gates. No source tree remains intentionally retained as an active
rollback runtime.
