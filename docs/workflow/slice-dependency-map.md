# Slice Dependency Map

## Linear Governance Chain

```text
Slice 00 -> Slice 01 -> Slice 02 -> Slice 03
```

- Slice 00 records current state.
- Slice 01 records target service boundaries, data ownership and rollback or
  strangler strategy.
- Slice 02 prepares the monorepo service structure without moving logic.
- Slice 03 creates contract-first communication material.

No service implementation slice may start before Slice 03 is complete.

## Service Implementation Chain

```text
Slice 03
  -> Slice 04 forensic-ingestion-service
  -> Slice 05 analysis-store-service
  -> Slice 06 repository-analysis-service
  -> Slice 07 java-ast-analysis-service
  -> Slice 08 joern-cpg-analysis-service
  -> Slice 09 btm-generation-service
  -> Slice 10 graph-replay-service
  -> Slice 11 report-generation-service
  -> Slice 12 forensic-gateway-service
  -> Slice 13 frontend-web-app
```

Service slices may be parallelized only after shared contracts are stable and
write scopes are disjoint. Persistence, gateway and frontend slices have
additional dependencies:

- Slice 05 must define owner APIs before other services use analysis-store data.
- Slice 10 must not directly access analysis-store persistence.
- Slice 11 must use analysis-store and graph-replay APIs only.
- Slice 12 depends on stable Gateway OpenAPI contracts.
- Slice 13 depends on Gateway API client boundaries.

## Runtime Environment Chain

```text
Service Dockerfiles -> Slice 14 local compose -> Slice 15 Swarm/Kubernetes
```

Compose, Swarm and Kubernetes work is blocked until service Dockerfiles and
healthcheck endpoints exist or the slice explicitly documents placeholders as
non-runtime planning artifacts.

## Verification Chain

```text
Slice 16 -> Slice 17 -> Slice 18 -> Slice 19 -> Slice 20
```

- Slice 16 creates cross-service contract and integration tests.
- Slice 17 removes obsolete monolith paths only after replacement evidence.
- Slice 18 performs the microservice readiness review.
- Slice 19 finalizes documentation.
- Slice 20 performs final diff, quality gate, commit and push if still
  authorized.

## Parallelization Limits

Allowed only after Slice 03:

- `java-ast-analysis-service`, `joern-cpg-analysis-service` and
  `btm-generation-service` may proceed in parallel if their contract files are
  frozen and their source write scopes do not overlap.
- Documentation updates may proceed beside service scaffolding only when they
  document completed or explicitly planned states without claiming unverified
  runtime evidence.

Forbidden:

- Parallel edits to the same Gradle settings or root build files without a
  single owner.
- Parallel service slices that modify the same contract file.
- Frontend implementation before Gateway contracts are stable.
