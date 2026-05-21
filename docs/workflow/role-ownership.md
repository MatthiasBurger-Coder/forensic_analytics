# Role Ownership

## Workflow Roles

| Role | Primary Responsibility |
|---|---|
| Senior Execution Orchestrator | Slice metadata, dependency order, locks, execution report and workflow handoff. |
| Senior Requirement Engineer | Requirement classification, EPIC drift, assumptions, non-goals and acceptance criteria. |
| Senior System Architect | ADR/arc42 reconciliation, service boundaries, module retirement safety and architecture stop conditions. |
| Microservice Senior Expert | Service autonomy, no shared Java implementation modules, no direct service project dependencies and runtime independence evidence. |
| Senior Java Backend Developer | Service-local Java implementation, hexagonal boundaries, ports, adapters and backend tests. |
| Senior gRPC/Proto Specialist | Protobuf/gRPC contracts, generated-code boundaries, validation, retries and compatibility. |
| Contract-First API Steward | REST/OpenAPI, gRPC, messaging and file contract governance. |
| Data Ownership and Persistence Steward | One-writer data ownership, persistence split, projections and cross-service read paths. |
| Senior Analysis Storage Architect | Canonical facts, artifacts, projections, correlation indexes and storage readiness. |
| Senior DevOps Engineer | Gradle build graph, Dockerfiles, Compose, runtime start, optional infrastructure and CI alignment. |
| Senior Security/Sandbox Engineer | Safe Git operations, workspace isolation, untrusted repository handling and secret leakage prevention. |
| Senior Tester | Regression plan, service tests, architecture tests, coverage and quality-gate evidence. |
| Senior Documentation Engineer | Workflow, ADR, arc42, README and architecture documentation synchronization. |
| Senior React Frontend Developer | N/A impact check unless public API changes affect frontend API adapters. |

## Slice Ownership

| Slice | Owner | Secondary Reviewers |
|---|---|---|
| S00 | Senior Execution Orchestrator | Requirement Engineer, System Architect, Tester |
| S01 | Senior System Architect | Requirement Engineer, Documentation Engineer, Microservice Senior Expert |
| S02 | Senior Java Backend Developer | System Architect, Microservice Senior Expert, Tester |
| S03 | Contract-First API Steward | Senior gRPC/Proto Specialist, Java Backend, Tester |
| S04 | Data Ownership and Persistence Steward | Analysis Storage Architect, System Architect, Security/Sandbox, Tester |
| S05 | Senior Java Backend Developer | Microservice Senior Expert, Security/Sandbox, DevOps, Tester |
| S06 | Senior gRPC/Proto Specialist | Java Backend, Contract Steward, Microservice Senior Expert, Tester |
| S07 | Senior Java Backend Developer | Source Analysis Pipeline, Microservice Senior Expert, Tester |
| S08 | Senior Joern CPG Specialist | Java Backend, Microservice Senior Expert, DevOps, Tester |
| S09 | Senior System Architect | Java Backend, Data Ownership, Microservice Senior Expert, Tester |
| S10 | Senior Java Backend Developer | Contract Steward, React Frontend impact check, Microservice Senior Expert, Tester |
| S11 | Senior Java Backend Developer | Contract Steward, Tester, Documentation Engineer |
| S12 | Senior DevOps Engineer | Observability Diagnostics, System Architect, Security/Sandbox, Tester |
| S13 | Senior Tester | DevOps, Microservice Senior Expert, Java Backend |
| S14 | Senior System Architect | Java Backend, Microservice Senior Expert, Tester, DevOps |
| S15 | Senior Tester | DevOps, Runtime Readiness, System Architect, Microservice Senior Expert |

## Subagent Note

Callable subagents were not used during workflow creation because the current
request did not explicitly ask for delegated or parallel agent work. During
workflow execution, use callable subagents only when the active runtime and user
request authorize it; otherwise apply the role files as local review checklists
and report that limitation.
