# Microservice Skill Sharpening Audit

## Decision

```text
READY_FOR_WORKFLOW
```

The Skill Registry and Conflict Auditor review may continue because the planned
microservice skills have explicit owners, bounded authority and documented
overlap with existing skills.

## Planned Skill Boundaries

| Planned skill | Owner | Primary authority | Existing overlap | Resolution |
| --- | --- | --- | --- | --- |
| `service-decomposition-bounded-context` | Service Decomposition / Bounded Context Expert | Evaluates whether a candidate boundary is a business service responsibility, owns data and can become independently deployable. | `microservice-senior-expert`, `architecture-modular-monorepo`, `data-ownership-persistence-steward` | New skill produces a Service Boundary Decision Record. Existing architecture and data skills remain reviewers for module dependencies and persistence ownership. |
| `contract-governance-expert` | Contract Governance Expert | Governs microservice-facing REST/OpenAPI, gRPC/protobuf and event contracts and blocks shared Java DTO or business-code coupling. | `contract-first-api-steward`, `protobuf-contracts`, `grpc-streaming-specialist` | New skill owns cross-service governance and coupling rules. Existing contract and protocol skills remain specialist reviewers for concrete REST, gRPC and protobuf changes. |
| `microservice-migration-safety-gate` | Microservice Senior Expert | Gates later production migration slices for scope, risk, rollback, strangler strategy and contract-first sequencing. | `microservice-senior-expert`, `migration-workflow`, `release-branch-governance` | New skill is a migration intake gate. Existing skills keep implementation, migration planning and release governance responsibilities. |
| `microservice-runtime-readiness-expert` | Senior DevOps Engineer | Verifies independent build, start, test, configuration, healthcheck, observability and container readiness before a candidate is called a microservice. | `devops-docker`, `devops-kubernetes`, `devops-ci-cd`, `quality-gate-governance` | New skill owns service readiness evidence. Deployment-specific skills remain reviewers and must verify Docker, Swarm or Kubernetes commands before documentation names them. |

## Implementation Status

| Skill | Status | Notes |
| --- | --- | --- |
| `service-decomposition-bounded-context` | Added | Defines bounded-context evaluation and outputs a Service Boundary Decision Record. |
| `contract-governance-expert` | Added | Defines cross-service contract governance and preserves specialist review boundaries. |
| `microservice-migration-safety-gate` | Added | Defines migration risk levels, rollback expectations and stop rules for later production migration slices. |
| `microservice-runtime-readiness-expert` | Added | Defines independent runtime evidence and records that Swarm/Kubernetes commands require future repository verification. |

## Conflict Classification

No blocking conflict remains after boundary assignment.

Non-blocking overlaps are expected because microservice migration governance
requires architecture, contract, DevOps, quality and release review. The
resolution is to keep each new skill as a gate or decision-record owner, while
existing specialist skills remain reviewers for concrete implementation details.

## Required Stop Rules For New Skills

- Stop when service ownership, data ownership or bounded context is unclear.
- Stop when a slice would introduce shared Java implementation, shared domain,
  shared DTO, shared service or shared repository modules between services.
- Stop when REST routes, RPC methods, event fields, error models or schema
  properties cannot be verified.
- Stop when runtime independence is claimed without build, start, test,
  configuration, healthcheck and container-readiness evidence.
- Stop when Docker, Docker Swarm, Kubernetes or CI commands are not verified
  from repository tooling.

## Execution Notes

- New skills belong under `.agents/skills/<name>/SKILL.md`.
- Skill audit documentation belongs under `docs/skill-audit/**`.
- Project-specific microservice governance must not be moved into portable
  `.codex` assets unless a later portability review explicitly approves it.
