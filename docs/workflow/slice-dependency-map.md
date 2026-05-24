# Slice Dependency Map: FA-MVP-0001

## Dependency Graph

```text
S00 Workflow Execution Preflight And Context Freeze
  -> S01 Requirement Terminology And Data Ownership Gate
    -> S02 Contract-First Workspace API And Owner API
      -> S03 Repository Source Workspace Domain And In-Memory Use Cases
        -> S04 Repository Metadata Resolution And Branch Checkout Refresh
          -> S05 H2 Dependency, Schema And Persistence Adapters
            -> S06 Repository Source gRPC Endpoint And Error Mapping
              -> S07 Query Report Public Workspace REST Facade
                -> S08 Forensic UI Create Workspace Flow
      S05 + S06 -> S09 Docker Local Volumes And Runtime Configuration
S07 + S08 + S09 -> S10 Security, Leakage, Idempotency And Restart Integration Gate
S10 -> S11 Documentation, arc42 And ADR Closure
S11 -> S12 Final Quality Gate And Workflow Handoff
```

## Parallelization Notes

Most slices are intentionally sequential because the contract and owner API
shape control backend, public REST and frontend work.

Limited parallelization is allowed only after S07:

- S08 frontend work and S09 Docker work may proceed in parallel if public DTOs
  and repository-source configuration names are frozen.
- S10 cannot start until S07, S08 and S09 have completed.

## Lock Summary

| Lock | Owning Slice |
|---|---|
| `docs/workflow/**` | S00, S10, S11, S12 |
| `contracts/openapi/**` | S02, S07 |
| `contracts/grpc/**` | S02, S06 |
| `services/repository-source-service/**` | S03, S04, S05, S06, S09, S10 |
| `services/query-report-api-service/**` | S07, S10 |
| `forensic-ui/**` | S08, S10 |
| `deployment/docker-compose/**` | S09 |
| `docs/architecture/**`, `docs/arc42/**`, `docs/adr/**` | S01, S09, S11 |
| `gradle/**` | S05 |

## Dependency Stop Conditions

Stop before implementation when:

- a dependency slice is incomplete or failed;
- file locks overlap without an explicit handoff;
- contract locks are not frozen before dependent UI or REST work;
- S3D detects cycles, missing metadata or unknown slice IDs during
  `workflow execute`.
