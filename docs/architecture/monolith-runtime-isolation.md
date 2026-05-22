# Monolith Runtime Isolation

## Status

Slice 18 records the current isolation decision for remaining
`forensic-analytics-*` runtime paths after the repository-to-BTM Gateway path
and frontend Gateway integration were implemented.

No `forensic-analytics-*` Gradle module is retired in Slice 18. Caller
verification still finds active in-process runtime and test dependencies, so
removing or disabling those modules would break verified current behavior.

FA-MSA-001 Slice 14 reaches the same retirement-readiness decision for the
current workflow state: direct legacy module removal is `NO_REMOVAL_SAFE`
until path-specific migration, explicit deprecation, caller-free scans and the
full quality gate are available.

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

The following remaining monolith runtime paths are retained only as legacy
in-process paths and rollback evidence:

| Path | Current role | Slice 18 decision |
|---|---|---|
| `forensic-analytics-cli` | Local in-process command adapter for analysis and engine-request import | Retained until a public CLI-to-Gateway command contract exists |
| `forensic-analytics-rest` | Current JDK HTTP adapter used by Boot and bootstrap paths | Retained as legacy REST behavior; not the target Gateway |
| `forensic-analytics-bootstrap` | Manual combined gRPC and REST runtime assembly | Retained as rollback/manual local runtime evidence |
| `forensic-analytics-boot-app` | Spring Boot wrapper for the current monolith adapters | Retained as legacy Boot runtime and rollback evidence |
| `forensic-analytics-engine` | In-process facade around application repository analysis use cases | Retained because CLI and testbed still verify this path |
| `forensic-analytics-ingestion-request` | JSON engine-request importer used by CLI | Retained until ingestion request behavior has a public Gateway or Ingestion service contract |
| `forensic-analytics-testbed` | In-process integration and architecture verification | Retained as monolith parity evidence until replacement tests exist |

These modules must not be described as implemented microservices. They share
Java implementation modules and remain inside the modular-monolith baseline.

## Retirement Preconditions

A later slice may retire or disable one of these paths only after it records:

- the verified replacement service owner;
- the exact public REST, gRPC or event contract;
- caller searches proving no remaining production or test dependency still
  needs the old path;
- parity or explicit deprecation tests;
- rollback instructions for restoring the current module registration and
  runtime entrypoint.

## Slice 19 Removal Review

Slice 19 did not remove shared implementation modules. The caller review still
finds active module registrations and dependencies in `settings.gradle.kts`,
`forensic-analytics-testbed`, `forensic-analytics-boot-app`,
`forensic-analytics-bootstrap`, `forensic-analytics-cli`,
`forensic-analytics-rest`, `forensic-analytics-engine` and
`forensic-analytics-ingestion-request`.

Because no module is both replaced and caller-free, deleting source roots or
editing Gradle registrations would violate the workflow stop condition for
Slice 19.

## Rollback

Rollback for Slice 18 is documentation-only: revert the Slice 18 documentation
commit. No runtime code, Gradle registration, source root, contract or
deployment descriptor is changed by this isolation decision.
