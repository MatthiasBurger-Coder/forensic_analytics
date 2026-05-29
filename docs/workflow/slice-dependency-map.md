# Slice Dependency Map

```mermaid
flowchart TD
    S01["S01 Governance Decision and Architecture Documents"]
    S02["S02 Gradle Dependencies and Typed PostgreSQL Configuration"]
    S03["S03 Liquibase Repository-Source Schema"]
    S04["S04 PostgreSQL Persistence Adapter"]
    S05["S05 Bootstrap, Liquibase Execution and Health Wiring"]
    S06["S06 Docker Compose and Local PostgreSQL Runtime"]
    S07["S07 H2 MVP Retirement and Migration Policy"]
    S08["S08 End-to-End Verification and Release Readiness"]

    S01 --> S02
    S02 --> S03
    S03 --> S04
    S04 --> S05
    S05 --> S06
    S06 --> S07
    S07 --> S08
```

## Parallelization Status

No implementation slices are parallel-safe. The persistence decision, build
configuration, Liquibase schema, adapter, bootstrap wiring, Docker runtime and
H2 retirement form one ordered cutover path.

## Critical Path

`S01 -> S02 -> S03 -> S04 -> S05 -> S06 -> S07 -> S08`

## Lock Summary

| Slice | Main Locks |
|---|---|
| S01 | ADR, arc42 and architecture documentation |
| S02 | Gradle dependency metadata and repository-source bootstrap config |
| S03 | Liquibase changelog resources |
| S04 | PostgreSQL outbound adapter and persistence tests |
| S05 | Repository-source bootstrap and health wiring |
| S06 | Docker PostgreSQL and repository-source Compose descriptors |
| S07 | H2 adapter/tests/docs retirement policy |
| S08 | Workflow execution evidence and final quality report |
