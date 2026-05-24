# Three Amigos Decision Record: FA-MVP-0001

## Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 88 percent.

## Normalized Requirement

Build the first MVP repository checkout workspace flow by extending the
existing `repository-source-service`, exposing sanitized public REST through
`query-report-api-service` and adding a `forensic-ui` Create Workspace flow.
Persist repository workspace, branch and idempotency state in service-local H2
for the Docker-local MVP. Do not introduce a new `workspace-service` or any
analysis pipeline behavior.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The user request is detailed and acceptance-oriented, but `FA-MVP-0001` is not present under `docs/epics`. Workflow may proceed by treating the user-provided requirement as the source requirement and recording the checkout-workspace assumption. |
| Senior System Architect | Repository checkout workspace ownership belongs to `repository-source-service`. Public REST belongs to `query-report-api-service`. H2 is service-local MVP persistence only. A new `workspace-service` is forbidden. |
| Senior Java Backend Developer | Existing symbols verify `prepare`, `get`, `cleanup`, `RepositoryPreparationRepository`, `RepositoryWorkspacePort` and `RepositoryCheckoutPort`. `Workspace`, `WorkspaceBranch`, `RepositoryIdentity`, H2 and branch refresh are new and must be introduced behind ports. |
| Senior React Frontend Developer | The UI has a suitable React/Vite boundary but no verified workspace create API. Frontend implementation must wait for contract-first public REST and must not infer metadata from URL strings. |
| Senior Tester | Regression-first slices are required for H2 persistence, idempotency replay/conflict, branch refresh, path leakage, public REST and UI behavior. Strict dependency verification remains mandatory. |

## Specialist Findings

- Contract Governance: public `/workspaces` routes currently conflict with
  OpenAPI tests that assert workspace fields are absent; S02 must replace that
  absence with explicit contract coverage.
- Data Ownership/Persistence: repository-source owns H2 tables and workspace
  paths; no cross-service database access is allowed.
- Security/Sandbox: repository input must remain HTTPS-only, no local/private
  targets, no submodules and no build execution.
- DevOps: H2 requires version catalog and verification metadata changes.
  Docker must create and own `/var/lib/forensic-analytics/repository-source-data`.
- Git Workspace: branch names must never become directory names and cleanup
  must remain inside the configured root.

## Accepted Assumptions

1. `Workspace` means repository-source checkout workspace state, not platform
   membership, authorization, project lifecycle or tenant administration.
2. H2 is an MVP adapter for durable repository-source state and is not the
   canonical production analytics store.
3. The user-provided FA-MVP-0001 requirement is the current source requirement
   until an EPIC file is added or linked by a later documentation slice.
4. Public API shape is not implemented yet and must be introduced contract-first.
5. Docker-local volume support does not imply Swarm, Kubernetes or production
   deployment readiness.

## S01 Terminology And Ownership Resolution

S01 resolves the documentation blocker by splitting the unqualified workspace
term into two explicitly separate concepts:

- Platform workspace: the deferred organizational, membership,
  authorization, project lifecycle, asset, audit and retention boundary.
- Repository checkout workspace: the FA-MVP-0001
  `repository-source-service` aggregate for one normalized repository identity,
  branch-level checkout state and source snapshot references.

Only the repository checkout workspace is in scope for FA-MVP-0001. It is
owned and written by `repository-source-service`; `query-report-api-service`
may expose sanitized public REST DTOs through owner APIs only. H2 remains a
repository-source Docker-local MVP adapter and does not resolve the broader
platform relational database decision.

## Blockers Resolved For Workflow Creation

- Branch isolation was established on
  `feature/workflow-repository-workspace-checkout-h2-persistence-20260524`.
- The previous `docs/workflow/**` content belonged to FA-MSA-001-LMR and was
  regenerated for this workflow branch.
- Callable subagents were available and used for read-only requirement,
  architecture, DevOps, workflow, backend, frontend and test reviews.

## Remaining Execution Stop Conditions

Workflow execution must stop if:

- repository docs contradict the checkout-workspace interpretation;
- contract-first public REST or owner API shape cannot be verified;
- H2 schema/table names are unclear after data ownership review;
- default branch resolution would require silently accepting missing branch and
  commit without contract approval;
- implementation would expose private paths or fabricate repository evidence.
