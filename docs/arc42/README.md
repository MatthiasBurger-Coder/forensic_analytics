# arc42 Architecture Documentation – Forensics Platform

This directory contains the arc42-based architecture documentation for the Forensics Platform.

## Source Baseline

This documentation is based on the EPIC:

- Name: Forensics Platform - Exception-Centered Runtime Replay and LLM-Assisted Failure Analysis
- Version: 0.2
- Date: 2026-05-17
- Status: Draft

## Sections

1. [Introduction and Goals](01-introduction-and-goals.md)
2. [Architecture Constraints](02-architecture-constraints.md)
3. [System Scope and Context](03-system-scope-and-context.md)
4. [Solution Strategy](04-solution-strategy.md)
5. [Building Block View](05-building-block-view.md)
6. [Runtime View](06-runtime-view.md)
7. [Deployment View](07-deployment-view.md)
8. [Crosscutting Concepts](08-crosscutting-concepts.md)
9. [Architecture Decisions](09-architecture-decisions.md)
10. [Quality Requirements](10-quality-requirements.md)
11. [Risks and Technical Debt](11-risks-and-technical-debt.md)
12. [Glossary](12-glossary.md)

## Documentation Principle

The EPIC remains the product and requirement baseline. The arc42 documentation transforms this baseline into an architectural structure.

## Governance Check Status

The Workspace Branch Selection workflow was created on branch
`feature/workflow-branch-selection-20260525`. The workflow stays inside the
FA-MVP-0001 repository checkout workspace scope and plans a guarded
frontend-first change: after a workspace is selected, branch choices may be
selected from that workspace's public `branches[]` records. It does not
introduce remote Git branch discovery, selected-branch persistence, new REST or
gRPC methods, repository-source persistence changes, platform workspace
administration, or analysis-pipeline behavior.

Workflow execution completed the guarded UI-only scope on the same branch. The
Workspaces list now keeps one row per repository checkout workspace, uses a
frontend-only selected branch intent derived from public `branches[]`, and
refreshes by the selected opaque `workspaceBranchId`.

Checked sections for this workflow:

- Building Block View: checked for repository-source ownership, query-report
  public facade boundaries and forensic-ui UI-state ownership.
- Runtime View: checked for `/workspaces` list and branch refresh behavior
  using existing public branch records.
- Crosscutting Concepts: checked for Repository Checkout Workspace versus
  Platform Workspace separation and branch names as data values.
- Architecture Decisions: checked against ADR-0010, ADR-0016 and ADR-0023; no
  new ADR is expected for UI-only branch-record selection.
- Quality Requirements: checked for deterministic branch selection, no-leak
  behavior and frontend regression expectations.
- Risks and Technical Debt: checked for remote branch discovery and selected
  branch persistence as future-risk topics outside this workflow.

The Workspaces Management View workflow was created on branch
`feature/workflow-workspaces-management-20260525`. The workflow stays inside
the FA-MVP-0001 repository checkout workspace scope and plans contract-first
list, branch refresh and safe cleanup/delete behavior. It does not introduce a
platform workspace administration service, hard-delete repository-source H2
provenance, or expand JavaParser, Joern, BTM, replay, report, graph, vector,
LLM, plugin or deployment behavior.

Checked sections for this workflow:

- Solution Strategy: checked for repository checkout workspace scope and
  no analysis-pipeline expansion.
- Building Block View: checked for repository-source ownership and query-report
  public facade boundaries.
- Runtime View: checked for planned `/workspaces` list and `/workspaces/new`
  create flow; concrete behavior updates are deferred until implementation.
- Crosscutting Concepts: checked for Repository Checkout Workspace versus
  Platform Workspace separation.
- Architecture Decisions: checked against ADR-0010, ADR-0013 and ADR-0023.
- Quality Requirements: checked for deterministic list, safe cleanup,
  idempotency and no-leak verification expectations.
- Risks and Technical Debt: checked for delete semantics and list scalability
  risks.

FA-MVP-0001 workflow execution reached the S11 documentation closure gate on
branch `feature/workflow-repository-workspace-checkout-h2-persistence-20260524`.
The architecture documentation now distinguishes the deferred platform
workspace administration concept from the repository-source-owned repository
checkout workspace used by FA-MVP-0001, records H2 as repository-source
Docker-local MVP persistence only, and keeps JavaParser, Joern, BTM, replay,
report, LLM, production database, Swarm and Kubernetes readiness outside this
MVP slice.

Checked sections for FA-MVP-0001 S11:

- Building Block View: checked for repository-source checkout workspace
  ownership and query-report public facade boundaries.
- Runtime View: checked for Create Workspace UI flow and public REST owner API
  path.
- Deployment View: checked for Docker-local repository-source workspace and H2
  volumes without Swarm or Kubernetes readiness claims.
- Crosscutting Concepts: checked for no private path, raw Git output, H2 path
  or credential leakage.
- Architecture Decisions: checked against ADR-0010, ADR-0013, ADR-0018 and
  ADR-0023; OD-001 remains open.
- Quality Requirements: checked for S10 H2 reopen, idempotency conflict,
  refresh and leakage regression scenarios.
- Risks and Technical Debt: checked for H2 and Docker-local readiness
  misunderstanding risks.

The Governance Performance Optimization workflow was checked for workflow
creation on branch `architecture/workflow-governance-performance-20260521`.
The workflow affects process-governance architecture only. It does not change
product runtime boundaries, product service responsibilities, deployment
topology, persistence ownership, contracts or evidence semantics.

Checked sections for this workflow:

- Architecture Constraints: checked for branch-first workflow creation,
  process-strand separation and quality authority.
- Solution Strategy: checked for profile-aware governance routing without
  weakening required gates.
- Building Block View: checked for skills, roles, routing, workflow and
  documentation-governance ownership.
- Runtime View: no product runtime flow change required.
- Deployment View: no deployment topology change required.
- Crosscutting Concepts: checked for context-pack, registry-cache and metrics
  provenance boundaries.
- Architecture Decisions: checked against ADR-0015, ADR-0016, ADR-0020 and
  ADR-0021.
- Quality Requirements: checked for profile-aware quality gates and mandatory
  STOP behavior.
- Risks and Technical Debt: checked for existing Flowchart Integrity Audit and
  S3D ownership gaps.
- Glossary: no term update required during workflow creation.

Later workflow-execute slices must update the relevant arc42 sections when
they close a documented governance gap or change accepted governance behavior.

Slice S05 closes the S3D ownership gap for this workflow branch by introducing
the dedicated Senior Execution Orchestrator and `s3d-execution-orchestrator`
skill. Senior Swarm Orchestrator remains the coordination owner.

Slice S08 closes the Flowchart Integrity Audit gap for this workflow branch by
introducing `flowchart-integrity-auditor` and routing flowchart integrity
checks through that dedicated skill. Senior Documentation Engineer remains the
documentation synchronization owner and Senior System Architect remains the
architecture-governance escalation owner.

The three-strand agent and workflow governance model was checked and
synchronized for Governance Flowchart V2 workflow execution on branch
`architecture/workflow-governance-flowchart-v2-20260517`. That branch is now
historical governance context.

Historical checked workflow context: the EPIC v0.2 alignment workflow ran on
branch `docs/workflow-forensics-tracing-analytics-epic-alignment-20260516`.

Checked sections:

- Introduction and Goals: no product goal change required.
- Architecture Constraints: checked for agent and workflow governance constraints.
- System Scope and Context: no runtime system boundary change required.
- Solution Strategy: checked for repository governance strategy.
- Building Block View: checked for repository governance building blocks.
- Runtime View: checked for repository governance runtime flows.
- Deployment View: no deployment topology change required.
- Crosscutting Concepts: checked for engineering governance and documentation synchronization.
- Architecture Decisions: checked for active workflow and agent governance decisions.
- Quality Requirements: checked for governance quality scenarios.
- Risks and Technical Debt: checked for governance drift, checkpoint and push automation risks.
- Glossary: checked for governance terms.
- ADR-0021: added for Governance Flowchart V2.

The checked workflow create end state is:

1. no blocking requirement questions remain,
2. complete checked docs/workflow/workflow.md,
3. checked or updated arc42 documentation,
4. Documentation Governance passed,
5. explicit release for workflow execute.

Reviewer role: Senior System Architect / arc42 Architecture Governance.

## Agent Governance Documentation Status

The agent organigramm and process-strand model are documented in:

- `docs/agents/organigramm.md`
- `docs/agents/agent-governance.md`
- `docs/agents/skill-registry.md`
- `docs/process/skills-update.md`
- `docs/process/workflow-create.md`
- `docs/process/workflow-execute.md`
- `docs/process/push-auto.md`
- `docs/governance/workflow/`

arc42 sections updated for agent governance:

- Architecture Constraints
- Solution Strategy
- Building Block View
- Runtime View
- Crosscutting Concepts
- Architecture Decisions
- Quality Requirements
- Risks and Technical Debt
- Glossary

Historical governance workflow branch:
`architecture/workflow-governance-performance-20260521`.
