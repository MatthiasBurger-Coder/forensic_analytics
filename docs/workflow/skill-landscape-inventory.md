# Skill Landscape Inventory

## Scope

This inventory is Slice 00 of the active skill landscape expansion workflow.

Inspected paths:

- `AGENTS.md`
- `QUALITY.md`
- `.agents/`
- `.codex/`
- `docs/workflow/`
- `docs/workplan/`
- `docs/adr/`
- `docs/skill-audit/`

## Active Workflow State

The active workflow location is `docs/workflow/`.

Current active files:

- `docs/workflow/README.md`
- `docs/workflow/skill-landscape-inventory.md`
- `docs/workflow/workflow.md`

Existing `docs/workplan/**` material remains present as historical or migration planning material. It is not the active workflow location for `workflow execute`.

## Existing Governance Sources

| Area | Verified artifact | Notes |
| --- | --- | --- |
| Repository agent rules | `AGENTS.md` | Defines `workflow execute`, active workflow discovery, mandatory subagent or role-review workflow and evidence-first safety rules. |
| Quality contract | `QUALITY.md` | Defines the minimum and full local Gradle quality gates plus documentation and evidence-integrity expectations. |
| Reusable execution rules | `.codex/workflow/workflow-execution-rules.md` | Defines the reusable workflow phases and stop conditions. |
| Project routing | `.agents/orchestrator/routing-rules.md` | Routes workflow generation, requirement gatekeeping, architecture, quality, security and documentation responsibilities. |
| Project orchestration | `.agents/orchestrator/swarm-orchestrator.md` | Defines coordination, ownership and stop rules for multi-role work. |
| Governance docs | `docs/governance/README.md` | Documents the workflow lifecycle and synchronization model. |
| Skill audit | `docs/skill-audit/**` | Records existing skill audit findings and previous conflict resolution notes. |

## Existing Skills

The repository currently contains 57 `.agents/skills` entries. Only `three-amigos-requirement-gatekeeper` currently has supporting files beyond a single `SKILL.md`.

The existing skill landscape includes:

- workflow and execution skills: `workflow-authoring`, `workflow-executor`, `workflow-slice`, `workflow-slice-execution`, `workflow-conflict-resolution`
- requirement and governance skills: `three-amigos-requirement-gatekeeper`, `requirement-engineering`, `engineering-governance`, `arc42-architecture-governance`, `documentation-sync`
- architecture skills: `architecture-hexagonal`, `architecture-modular-monorepo`, `architecture-archunit-hexagonal`, `microservice-senior-expert`
- quality skills: `quality-gate`, `quality-gate-governance`, `quality-architecture-validation`, `quality-archunit-review`, `quality-testing-strategy`, `testing-junit6`
- backend and integration skills: `java-25-backend`, `grpc-ingestion`, `grpc-streaming-specialist`, `protobuf-contracts`, `source-analysis-pipeline`
- platform skills: `workspace-lifecycle-specialist`, `git-large-repository-specialist`, `analysis-storage-architect`, `distributed-systems-architect`, `performance-scalability-engineer`
- review skills: `analytics-persistence-review`, `ingestion-handoff-review`, `joern-semantic-analysis`, `replay-graph-llm-review`
- security and observability skills: `security-sandbox-specialist`, `observability-diagnostics`, `resilience-engineering`

Existing skills provide useful foundations but do not fully cover the target governance steward skills requested by the active workflow.

## Existing Roles And Callable Agents

Project roles exist under `.agents/roles`, including:

- `senior-system-architect`
- `senior-workflow-architect`
- `senior-swarm-orchestrator`
- `senior-requirement-engineer`
- `senior-documentation-engineer`
- `senior-java-backend`
- `senior-devops`
- `senior-tester`
- `microservice-senior-expert`
- `senior-grpc-proto-specialist`
- `senior-analysis-storage-architect`
- `senior-security-sandbox-engineer`

Callable Codex agent definitions exist under `.codex/agents`, including matching senior architecture, workflow, quality, security, DevOps, backend and reviewer agents.

Inventory counts:

- `.agents/roles`: 18 role entries, including flat Markdown roles and role directories with `SKILL.md`.
- `.codex/agents`: 34 callable-agent TOML definitions.
- `.codex/subagents`: 9 durable role descriptions.
- `.codex/skills`: 6 reusable skills.

## Missing Target Skills

The following target skill directories were not present during Slice 00 inventory:

| Target skill | Status |
| --- | --- |
| `.agents/skills/skill-registry-conflict-auditor/` | Missing |
| `.agents/skills/agent-handoff-protocol/` | Missing |
| `.agents/skills/contract-first-api-steward/` | Missing |
| `.agents/skills/data-ownership-persistence-steward/` | Missing |
| `.agents/skills/quality-gate-orchestrator/` | Missing |
| `.agents/skills/adr-steward/` | Missing |
| `.agents/skills/security-threat-modeling/` | Missing |
| `.agents/skills/observability-runtime-diagnostics/` | Missing |
| `.agents/skills/release-branch-governance/` | Missing |

`three-amigos-requirement-gatekeeper` already exists and is the target of Slice 03 refinement rather than new creation.

## Existing ADRs

The repository already contains ADRs under `docs/adr/`:

- `ADR-0001-plugins-are-producers.md`
- `ADR-0002-canonical-analysis-model.md`
- `ADR-0003-runtime-events-are-sensitive.md`
- `ADR-0004-graph-and-vector-db-as-projections.md`
- `ADR-0005-adapter-logging-observability-boundary.md`
- `ADR-0006-spring-boot-server-boundary.md`
- `ADR-0007-rest-api-spring-strategy.md`
- `ADR-0008-cross-cutting-logging-module.md`

The initial ADR backlog requested by the workflow uses a different numeric file naming scheme. Slice 14 must avoid overwriting existing ADR intent and should add new files only where their decisions are not already covered by existing ADRs.

## Existing Codex Prompt State

`.agents/prompts/` and `docs/workflow/prompts/` are not currently present. Slice 12 may create them if prompt integration remains required after earlier governance skills exist.

`.codex/workflow/` exists and currently contains reusable workflow execution rules.

## Potential Conflicts And Overlaps

| Area | Classification | Notes |
| --- | --- | --- |
| `docs/workplan` vs `docs/workflow` | Non-blocking workflow-location overlap | `docs/workflow` is now active. `docs/workplan` remains historical or migration planning material. |
| `observability-diagnostics` vs target `observability-runtime-diagnostics` | Non-blocking responsibility overlap | Existing skill covers diagnostics broadly. Target skill should own governance rules for trace context, logging and metrics across workflows. |
| `security-sandbox-specialist` vs target `security-threat-modeling` | Non-blocking responsibility overlap | Existing skill focuses on sandboxing and safe repository handling. Target skill should own broader threat modeling and security review workflow. |
| `quality-gate` / `quality-gate-governance` vs target `quality-gate-orchestrator` | Non-blocking responsibility overlap | Existing skills identify or govern quality commands. Target skill should orchestrate slice and commit gate evidence. |
| `analysis-storage-architect` vs target `data-ownership-persistence-steward` | Non-blocking responsibility overlap | Existing skill focuses on storage architecture. Target skill should own service-level data ownership and cross-store decision governance. |
| Existing ADRs vs Slice 14 ADR backlog | Requires care | New ADRs must not rewrite or contradict existing ADR history. |
| Missing early reviewer skills | Bootstrap conflict, non-blocking with role fallback | The workflow assigns Slice 00 review to target skills that do not exist yet. Until Slice 02 and Slice 07 create them, use existing Senior System Architect, Senior Tester, quality-gate and repository-explorer roles as fallback reviewers. |
| `.agents` and `.codex` duplicate skill names | Non-blocking layering overlap | `workflow-executor` exists in both `.agents/skills` and `.codex/skills`. Root and project-specific instructions take precedence over reusable `.codex` behavior. |
| Planned ADR file names | Requires correction before Slice 14 | Existing ADRs use `ADR-0001-*` through `ADR-0008-*`. New ADRs must preserve this naming convention and avoid number reuse. |

## Initial Blocker Review

Slice 00 role review found bootstrap conflicts in the first workflow draft:

- early slices referenced target reviewer skills before those skills existed;
- documentation-only checks were worded as if `git diff --check` could replace `QUALITY.md`;
- the hierarchy needed to keep Agent Workflow Orchestrator above workflow execution while Senior System Architect owns architecture governance;
- project-specific prompt integration originally targeted portable `.codex` files;
- initial ADR backlog names needed to preserve the existing `ADR-000x-*` convention.

These were corrected in `docs/workflow/workflow.md` before continuing into Slice 01.

No blocking conflict remains for Slice 01.

Required constraints for later slices:

- Keep the active workflow under `docs/workflow`.
- Keep old `docs/workplan` material untouched unless a later slice explicitly owns migration or archival.
- Do not overwrite existing ADRs.
- Add new governance skills with explicit boundaries so they complement, rather than replace, existing skills.
- Keep commit and push disabled unless Slice 16 confirms explicit permission and quality-gate status.
- Treat failed required quality gates as blocking. Only optional, unavailable or not-applicable checks may be documented as non-blocking.

## Slice 00 Verification

- Inventory completed from repository files.
- Existing governance sources identified.
- Missing target skills identified.
- Potential overlaps documented.
- No production code was changed.
