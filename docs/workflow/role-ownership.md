# Role Ownership

## Primary Routing

| Slice | Primary Owner | Secondary Reviewers |
|---|---|---|
| S00 | Senior Execution Orchestrator | Senior Requirement Engineer, Senior System Architect, Senior Tester |
| S01 | Senior System Architect | Senior Requirement Engineer, Contract-First API Steward, Senior DevOps, Senior Tester |
| S02 | Senior DevOps | Senior System Architect, Contract Governance Expert, Senior React Frontend, Senior Tester |
| S03 | Senior Tester | Senior Java Backend, Microservice Senior Expert, Senior DevOps, Senior System Architect |
| S04 | Senior Documentation Engineer | Senior DevOps, Senior System Architect, Senior Tester, Microservice Runtime Readiness Expert |
| S05 | Senior Java Backend | Senior DevOps, Senior System Architect, Microservice Senior Expert, Senior Tester |
| S06 | Senior System Architect | ADR Steward, Senior Documentation Engineer, Senior Requirement Engineer, Senior Tester |
| S07 | Senior DevOps | Senior Tester, Senior System Architect, Microservice Runtime Readiness Expert, Senior Documentation Engineer |

## Mandatory Review Rules

- Senior System Architect reviews every slice because the workflow changes the
  final modular-monolith retirement state.
- Microservice Senior Expert reviews source deletion and service-autonomy
  claims.
- Senior Tester reviews every slice with tests, source deletion, quality-gate
  or regression-coverage impact.
- Senior DevOps reviews Gradle, Docker, runtime, build and release-readiness
  material.
- Contract specialists review every REST/OpenAPI, gRPC/protobuf, CLI or file
  contract wording change before implementation depends on it.
- Senior React Frontend reviews only when public API fields, endpoints,
  response status shapes or `forensic-ui` API mappers change.
- ADR Steward reviews final retirement ADR creation or supersession.

## Callable Subagent Policy

Callable subagents were used for workflow creation because the user explicitly
requested subagent participation. During `workflow execute`, callable subagents
should be used when available and explicitly authorized by the active runtime.
When a callable subagent is unavailable, the matching role file or skill is used
as a local review checklist and the limitation is recorded in
`docs/workflow/execution-report.md`.
