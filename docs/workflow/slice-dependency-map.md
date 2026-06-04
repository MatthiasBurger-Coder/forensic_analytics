# Slice Dependency Map

```text
S01 Requirement And ADR Alignment Documentation
  -> S02 Repository-Source Persistence Boundary Verification
      -> S03 Public API And Frontend Boundary Verification
      -> S04 Docker-Local PostgreSQL And Volume Boundary Verification
          -> S05 Final Quality, Arc42 And Handoff Closure
```

## Parallelization

S03 and S04 may be reviewed in parallel after S02 because their implementation
and verification scopes are disjoint. S05 waits for all earlier slices.

## Locks

| Slice | File Locks | Contract Locks | Architecture Locks |
|---|---|---|---|
| S01 | `docs/workflow/**`, selected `docs/architecture/**`, selected `docs/arc42/**` | none | ADR-0023, ADR-0024 |
| S02 | `repository-source-service/**` persistence and bootstrap files | none | repository-source owns PostgreSQL metadata; H2 tests only |
| S03 | `query-report-api-service/**`, `forensic-ui/**` | workspace REST DTOs, repository-source owner gRPC API | query-report facade only; UI public REST only |
| S04 | repository-source Dockerfile, Compose descriptors, deployment docs | none | private workspace volume, PostgreSQL metadata schema |
| S05 | `docs/workflow/**`, `docs/arc42/**`, `docs/architecture/**` | none | final PostgreSQL/H2 boundary |

## Deadlock Checks

- No slice depends on itself.
- No dependency points backward to a later slice.
- Shared documentation closure is isolated in S05.
- Product implementation is not authorized by this workflow-create artifact.
