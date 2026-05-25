# Three Amigos Decision Record

## Requirement Summary

Fix the observed local WSL WildFly checkout problem, prove the fix with public
API `curl` calls, and start the local MVP system for manual trial after the
proof succeeds.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The requirement is specific enough for workflow execution. It is a local runtime bug fix, not a new product analysis capability. |
| Senior System Architect | The fix must preserve repository-source ownership of checkout paths and query-report's public facade boundary. No contract change is required unless execution proves otherwise. |
| Senior Java Backend Developer | Two bounded backend changes are planned: repository-source bootstrap workspace-root defaulting and query-report HTTP lifecycle executor behavior. |
| Senior React Frontend Developer | No frontend production code is planned. Frontend involvement is limited to building/serving existing UI assets for the final manual trial. |
| Senior Tester | Regression tests must be deterministic and not depend on real WSL or real GitHub. The WildFly checkout is an integration proof, not a unit-test dependency. |

## EPIC Alignment

EPIC v0.2 is the relevant requirement source. The workflow aligns with it
because it changes local runtime preparation and API availability only. It does
not redefine canonical analysis semantics, source facts, runtime observations,
replay truth, graph projections, reports or LLM evidence packages.

## Architecture And Evidence Validation

- WildFly checkout output is operational proof, not forensic analysis evidence.
- The API proof must use public DTOs only.
- Missing or slow checkout evidence must remain explicit and must not be
  converted into a successful checkout.
- The workflow must not execute checked-out repository code.

## Quality And Verification Validation

- Targeted service tests are required before live proof.
- The repository minimum quality gate is required after implementation.
- Full local quality gate is required for commit readiness.
- Curl proof must record exact endpoints, status, workspace ID, branch status,
  resolved commit and live workspace root.

## Dependency Summary

S01 precedes all implementation. S02 and S03 can run in parallel. S04 depends
on both S02 and S03. S05 depends on S04.

## Open Questions

None blocking for workflow execution. The executor must choose unused local
ports at runtime and record them.

## Final Decision

`READY_FOR_WORKFLOW`

Confidence: 92 percent.
