# Three Amigos Decision Record

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-microservice-decomposition-20260521-v1` |
| Requirement ID | `FA-MSA-001` |
| Decision | `READY_FOR_WORKFLOW` |
| Confidence | 90 percent |
| Execution profile | `FULL_PATH` |

## Normalized Requirement

Fully dissolve the current `forensic-analytics-*` modular monolith into real,
independently buildable, startable and containerized services. Productive
services must own local domain, application, adapter, bootstrap and persistence
boundaries. Services may communicate only through REST, gRPC, messaging or
documented file contracts. No productive service may depend on central shared
Java modules such as domain, application, persistence, logging, bootstrap or
boot app modules.

## Requirement Classification

- Functional requirement: service-local extraction of repository source,
  ingestion, JavaParser, Joern, orchestration, query/report API and CLI
  behavior.
- Architecture constraint: no shared Java implementation modules and no direct
  service-to-service Gradle project dependencies.
- Quality-gate requirement: per-slice targeted tests, repository minimum gate
  for production changes and full local quality gate before old module removal.
- Data ownership requirement: one owner and one write path for every persistent
  data category.
- Deployment requirement: every productive service has a Dockerfile and
  documented start path before readiness is claimed.
- Security requirement: repository workspaces, runtime values, credentials and
  diagnostics remain isolated and redacted.
- Observability requirement: service-local diagnostics and deployment
  observability, not a shared Java logging module.
- Assumption: FA-MSA-001 supersedes the existing target service naming
  direction, but Slice 01 must formalize that in ADR and arc42 before product
  migration.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | Requirement is explicit enough for workflow creation. It conflicts with existing target naming in ADR-0017 and arc42, so Slice 01 must reconcile architecture docs first. |
| Senior System Architect | APPROVE FOR WORKFLOW with `FULL_PATH`. Service-boundary and no-shared-code rules are clear. Product migration must stop until ADR/arc42 target naming is updated. |
| Senior Java Backend Developer | APPROVE FOR WORKFLOW. Backend slices must move one service responsibility at a time, keep generated contract code service-local and preserve monolith callers until parity tests exist. |
| Senior React Frontend Developer | N/A impact for workflow creation. Recheck during query/report API work if public API fields consumed by frontend code change. |
| Senior Tester | APPROVE FOR WORKFLOW. Every production slice needs targeted tests and `git diff --check`; old module retirement requires the full local quality gate. |

## Specialist Findings

| Specialist | Finding |
|---|---|
| Microservice Senior Expert | Current modules and partial service slices are not sufficient proof of microservice readiness. Each target service needs independent build, start, test, Dockerfile and no shared Java module dependency evidence. |
| Contract-First API Steward | S03 must define external contracts before service implementation depends on communication behavior. Generated Java DTOs must not be shared. |
| Data Ownership and Persistence Steward | S04 must settle ownership for canonical facts, orchestration state, report artifacts and service-local persistence before `forensic-analytics-persistence` removal. |
| Senior DevOps Engineer | Docker readiness can be claimed only from service-local Dockerfiles and verified commands. Docker Swarm and Kubernetes readiness remain unclaimed until manifests exist. |
| Senior Security/Sandbox Engineer | Repository checkout and Joern execution must not leak private workspaces or execute untrusted project code without an approved sandbox decision. |

## Architecture And Evidence Integrity Validation

- Existing ADR-0017 and arc42 target names differ from FA-MSA-001.
- Existing documentation records active callers for old monolith modules.
- Static analysis and Joern facts must remain static/semantic evidence, not
  runtime execution proof.
- Query/report and LLM-facing outputs must label hypotheses and generated text
  separately from verified evidence.

## Dependency And Deadlock Validation

The workflow is acyclic:

```text
S00 -> S01 -> S02 -> S03/S04 -> service extraction slices -> retirement -> closure
```

S05 through S08 have potential parallel implementation only after S03 and S04,
but the active workflow keeps execution sequential unless the user explicitly
authorizes parallel subagent or worker execution with disjoint write scopes.

## Open Questions

- Optional services named by FA-MSA-001, such as `btm-generation-service`,
  `graph-replay-service` and `incident-analysis-service`, are not mandatory for
  closure unless a later requirement update makes them mandatory.

## Blockers

No blocker prevents workflow creation. Product implementation slices are blocked
until:

- S01 resolves ADR/arc42 target naming.
- S04 resolves data ownership.
- S03 defines or reconciles required external contracts.

## Decision

`READY_FOR_WORKFLOW`
