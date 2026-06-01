# Slice Dependency Map

```mermaid
flowchart TD
    S01["S01 Governance Decision and Architecture Documents"]
    S02["S02 Gradle Dependencies and Typed PostgreSQL Configuration"]
    S03["S03 Liquibase Repository-Source Schema"]
    S04["S04 PostgreSQL Persistence Adapter"]
    S05["S05 Bootstrap, Liquibase Execution and Health Wiring"]
    S06["S06 Docker Compose and Local PostgreSQL Runtime"]
    S07["S07 PostgreSQL Runtime Default and H2 Test Boundary"]
    S08["S08 Database Settings Contract and Backend Handoff"]
    S09["S09 React Database Settings UI"]
    S10["S10 End-to-End Verification and Release Readiness"]

    S01 --> S02
    S02 --> S03
    S03 --> S04
    S04 --> S05
    S05 --> S06
    S06 --> S07
    S07 --> S08
    S08 --> S09
    S09 --> S10
```

## Parallelization Status

No implementation slices are parallel-safe. The persistence decision, build
configuration, Liquibase schema, adapter, bootstrap wiring, Docker runtime and
H2 test-boundary cutover, Settings contract and Settings UI form one ordered
cutover path.

## Critical Path

`S01 -> S02 -> S03 -> S04 -> S05 -> S06 -> S07 -> S08 -> S09 -> S10`

## Lock Summary

| Slice | Main Locks |
|---|---|
| S01 | ADR, arc42 and architecture documentation |
| S02 | Gradle dependency metadata and repository-source bootstrap config |
| S03 | Liquibase changelog resources |
| S04 | PostgreSQL outbound adapter and persistence tests |
| S05 | Repository-source bootstrap and health wiring |
| S06 | Docker PostgreSQL and repository-source Compose descriptors |
| S07 | Runtime persistence defaults, H2 test boundary and documentation |
| S08 | Public Settings API, repository-source handoff and security/ownership rules |
| S09 | React Settings page, API adapter and frontend state |
| S10 | Workflow execution evidence and final quality report |
