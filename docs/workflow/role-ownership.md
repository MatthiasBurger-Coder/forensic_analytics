# Role Ownership

## Primary Routing

| Slice | Primary Owner | Secondary Reviewers |
|---|---|---|
| S00 | Senior Execution Orchestrator | Senior Requirement Engineer, Senior System Architect, Senior Tester |
| S01 | Senior System Architect | Microservice Senior Expert, Senior Java Backend, Senior Tester |
| S02 | Contract-First API Steward | Senior gRPC/Proto Specialist, Senior System Architect, Senior Tester, Senior DevOps |
| S03 | Senior Java Backend | Senior Git Workspace Specialist, Microservice Senior Expert, Security/Sandbox, Senior Tester |
| S04 | Senior Java Backend | Senior gRPC/Proto Specialist, Ingestion Handoff Review, Microservice Senior Expert, Senior Tester |
| S05 | Senior Java Backend | Source Analysis Pipeline, Microservice Senior Expert, Senior Tester |
| S06 | Senior Joern CPG Specialist | Senior Java Backend, Senior DevOps, Microservice Senior Expert, Senior Tester |
| S07 | Senior Java Backend | Distributed Systems Architect, Data Ownership Steward, Microservice Senior Expert, Senior Tester |
| S08 | Senior Java Backend | Contract Governance Expert, Senior DevOps, Senior React Frontend, Microservice Senior Expert, Senior Tester |
| S09 | Senior Java Backend | Contract Governance Expert, Senior UX Designer, Senior Tester |
| S10 | Senior DevOps | Observability Runtime Diagnostics, Senior Java Backend, Security Threat Modeling, Senior Tester |
| S11 | Senior Analysis Storage Architect | Data Ownership Steward, Senior Java Backend, Microservice Senior Expert, Senior Tester |
| S12 | Senior System Architect | Senior Java Backend, Microservice Senior Expert, ArchUnit Review, Senior Tester |
| S13 | Senior Tester | Microservice Senior Expert, Senior DevOps, Senior Java Backend |
| S14 | Senior DevOps | Senior System Architect, Senior Java Backend, Microservice Senior Expert, Senior Tester |
| S15 | Senior System Architect | Senior DevOps, Senior Tester, Runtime Readiness Expert, Senior Documentation Engineer |

## Mandatory Review Rules

- Microservice Senior Expert reviews every slice that removes a legacy module
  or changes service autonomy.
- Senior Tester reviews every slice with code, tests, Gradle or deletion
  impact.
- Senior DevOps reviews every slice that changes Gradle, Docker, runtime start,
  healthcheck or deployment material.
- Contract specialists review every REST, gRPC, event, CLI or file-contract
  change before implementation depends on the contract.
- Data Ownership and Persistence Steward reviews all persistence and stored
  evidence decisions.
- Senior React Frontend Developer is an impact reviewer when public API fields,
  endpoints, status models or client-visible error shapes change.

## Callable Subagent Policy

Callable subagents are used during `workflow execute` only when explicitly
authorized by the runtime and current user request. When callable subagents are
not used, the matching role files and skills are used as explicit local review
checklists and the limitation is recorded in the execution report.
