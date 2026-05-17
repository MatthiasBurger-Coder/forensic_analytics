# Role Ownership

## Workflow Create Reviews

Callable subagents were used for read-only workflow-create review because the
request explicitly required subagents or roles.

| Role | Workflow-create finding |
|---|---|
| Senior Requirement Engineer | Regenerate the stale Governance Flowchart V2 workflow and create EPIC v0.2 instead of editing v0.1 in place. |
| Senior System Architect | Preserve the plugin-as-producer boundary, do not encode producer port or RPC defaults, and add a contract-governance checkpoint. |
| Senior Java Backend Developer | Keep backend work out of scope and stop if product paths appear. |
| Senior React Frontend Developer | Keep frontend work out of scope and avoid implemented UI claims for graph, replay or LLM behavior. |
| Senior Tester | Add executable per-slice acceptance criteria, expanded leakage searches, marker scans and quality-command reporting. |
| Senior Documentation Engineer | Update docs references after EPIC v0.2 and preserve historical ADRs. |

## Required Execution Roles

| Responsibility | Role or skill |
|---|---|
| Workflow orchestration | Agent Workflow Orchestrator / Senior Workflow Architect |
| Requirement gap analysis | Three Amigos Requirement Gatekeeper |
| EPIC wording | Senior Requirement Engineer |
| Platform boundaries | Senior System Architect |
| Contract neutrality | Contract-First API Steward |
| Data ownership | Data Ownership & Persistence Steward |
| Runtime data sensitivity | Security & Threat Modeling |
| Testability and quality | Senior Tester |
| Skill/governance conflicts | Skill Registry & Conflict Auditor |
| Documentation consistency | Senior Documentation Engineer |
| Backend impact | Senior Java Backend Developer |
| Frontend impact | Senior React Frontend Developer |

## Execution Rules

- Subagents must verify the active branch before edits.
- Subagents must not switch branches.
- Write ownership must be explicit per slice.
- Read-only comparison work may run in parallel only when no write locks overlap.
- If callable subagents are unavailable during execution, the matching role or
  skill file must be used as a checklist and the limitation must be reported.
