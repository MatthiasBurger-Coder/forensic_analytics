# Three Amigos Decision Record

## Decision

`READY_FOR_WORKFLOW`

## Requirement Summary

Create a controlled workflow for completing the Forensic Analytics
microservice migration and the first repository-to-BTM pipeline. The target
behavior is:

```text
Plugin / external client
  -> Gateway HTTP API with a Git repository request
  -> service-owned repository workspace and source snapshot
  -> analysis worker services
  -> generated BTM files
  -> gRPC delivery of completed BTM files
```

## Requirement Classification

| Type | Classification |
|---|---|
| Functional requirement | Submit external Git repositories, create workspaces, generate BTM rules, deliver BTM files over gRPC. |
| Architecture constraint | Complete microservice migration without shared Java implementation modules. |
| Security requirement | Reject unsafe Git remotes and prevent workspace path leakage. |
| Resilience requirement | Use idempotency, bounded timeouts, size limits and explicit incomplete states. |
| Quality requirement | Run service-local tests, contract tests, ArchUnit checks and `QUALITY.md` gates. |
| UX/API requirement | Plugin and frontend use Gateway/public API surfaces only. |
| Evidence requirement | Generated rules remain generated instrumentation, not runtime evidence. |

## Mandatory Role Findings

| Role | Finding |
|---|---|
| Senior Requirement Engineer | EPIC v0.2 supports the requested direction. Producers are request and runtime-binding adapters; Analytics owns server-side analysis and BTM generation. |
| Senior System Architect | ADR-0017 defines the service landscape. ADR-0009 and ADR-0010 block shared Java service modules and require contract-first service communication. |
| Senior Java Backend Developer | Service-local implementations exist for repository analysis and BTM generation, but Gateway facade integration, artifact delivery and worker-chain integration are missing. |
| Senior React Frontend Developer | Frontend impact waits for Gateway readiness. The UI must not call internal worker services directly. |
| Senior Tester | Acceptance requires incremental tests: contract tests first, then service-local tests, then end-to-end repository-to-BTM verification and full quality gate. |

## Subagent Review Integration

Callable subagents were used for the mandatory workflow-create perspectives.
Their blockers were integrated before release: workflow history restoration,
slice-scoped checkpoint governance, Gateway idempotency, BTM byte delivery
ownership, frontend Gateway-only gates, contract-test stop rules, runtime
readiness gates and stale slice-number cleanup.

## Service Boundary Decision

| Field | Decision |
|---|---|
| Candidate boundary | Existing ADR-0017 target landscape. |
| Primary target services | Gateway, Repository Analysis, Java AST Analysis, Joern CPG Analysis, Analysis Store, BTM Generation. |
| Business capability | Server-side repository analysis and BTM artifact generation for plugin-triggered instrumentation. |
| Owned data | See `docs/workflow/workflow.md` data ownership table. |
| Allowed communication | REST/OpenAPI, gRPC/protobuf and approved events only. |
| Forbidden coupling | Shared Java implementation, DTO, domain, mapper, repository, fixture or error-model modules. |
| Decision | Approved for workflow slices, not approved for big-bang implementation. |

## Contract Impact

The workflow must define or update:

- Gateway HTTP route for plugin/external Git repository submission.
- Job/status model for long-running BTM generation.
- gRPC BTM file delivery with bounded transfer semantics.
- Artifact metadata and byte ownership contracts.
- Error, retry, idempotency, timeout and cancellation behavior.

Contract changes must preserve existing field numbers and compatibility rules
in `docs/architecture/contract-versioning.md`.

## Data Ownership Impact

Analysis Store owns canonical job state and accepted artifact metadata.
Repository Analysis owns workspaces and source snapshots. BTM Generation owns
generated BTM bytes until an explicit byte-handoff, object-store ownership or
delivery contract transfers byte custody. Analysis Store registration transfers
accepted artifact metadata only. No service may read another service's private
database, private filesystem paths or generated classes.

## Test Impact

Expected test layers:

- contract tests for OpenAPI and gRPC semantics;
- service-local domain and application tests;
- gRPC endpoint tests;
- ArchUnit service-boundary tests;
- repository-workspace security tests;
- BTM determinism and artifact byte delivery tests;
- end-to-end repository-to-BTM integration tests;
- full `QUALITY.md` gate before migration acceptance.

## Risk Level

`HIGH`

Reasons:

- multi-service runtime flow;
- public API and gRPC contract changes;
- external Git repository handling;
- generated artifact delivery;
- eventual removal of modular-monolith modules;
- deployment and rollback implications.

## Rollback And Strangler Strategy

Keep current modular-monolith paths available until each replacement service
path has verified parity. Retire or remove legacy modules only after:

- the service replacement is implemented;
- targeted and full quality gates pass;
- consumers are migrated;
- rollback or deprecation behavior is documented.

## Acceptance Criteria

- Gateway HTTP accepts a clean HTTPS Git repository request with idempotency and
  correlation metadata.
- Repository Analysis creates a service-owned workspace and source snapshot
  without exposing private paths.
- Worker services produce accepted artifacts or explicit incomplete states.
- BTM Generation produces deterministic `.btm` and manifest bytes.
- The plugin-facing gRPC contract returns completed BTM files or explicit
  unavailable state.
- Service implementation modules are autonomous and do not share Java runtime
  code.
- Obsolete modular-monolith modules are removed only after verified service
  parity.
- Documentation, arc42 and ADRs match implementation status.
- The workflow version is recorded in `docs/workflow/workflow.history.md`.
- Every successful execution slice records a CP_RECORD entry, creates a
  slice-scoped checkpoint commit and pushes the workflow branch before the next
  slice starts.

## Open Questions

No blocking question remains for workflow creation. Non-blocking decisions are
assigned to early contract-first slices:

- exact gRPC BTM file delivery shape;
- artifact byte owner API;
- instrumentation target owner;
- final local deployment topology.

`workflow execute` is intentionally not started by this decision. It requires
the regenerated workflow-create package to be committed first, so the execution
preflight starts clean.

## Final Decision

`READY_FOR_WORKFLOW`
