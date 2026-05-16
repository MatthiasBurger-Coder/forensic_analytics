# Skill And Agent Inventory Baseline

This baseline records the read-only inventory used during workflow creation.
Slice 01 will create the full governance inventory at
`docs/governance/skill-agent-inventory.md`.

## Verified Core Files

- Root governance: `AGENTS.md`
- Quality contract: `QUALITY.md`
- Portable Codex team rules: `.codex/AGENTS.md`
- Workflow execution rules: `.codex/workflow/workflow-execution-rules.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Workflow create prompt: `.agents/prompts/workflow-create.md`
- Workflow execute prompt: `.agents/prompts/workflow-execute.md`
- Slice execute prompt: `.agents/prompts/slice-execute.md`

## Verified Role Areas

The repository contains role files for:

- Senior System Architect
- Senior Workflow Architect
- Senior Requirement Engineer
- Senior Swarm Orchestrator
- Senior Java Backend
- Senior React Frontend
- Senior UX Designer
- Senior DevOps
- Senior Tester
- Senior Documentation Engineer
- Microservice Senior Expert
- Senior gRPC / Proto Specialist
- Senior Security / Sandbox
- Senior Performance Engineer
- Senior Analysis Storage Architect
- Senior Joern CPG Specialist
- Senior Git Workspace Specialist
- Senior Plugin Integration Developer

## Verified Skill Convention

The repository uses this skill layout:

```text
.agents/skills/<skill-name>/SKILL.md
```

Execution must not create flat files such as
`.agents/skills/<skill-name>.md` unless a dedicated governance decision changes
the repository convention.

## Existing Governance Skills Relevant To This Workflow

- `workflow-authoring`
- `workflow-executor`
- `workflow-slice`
- `workflow-slice-execution`
- `workflow-conflict-resolution`
- `three-amigos-requirement-gatekeeper`
- `skill-registry-conflict-auditor`
- `engineering-governance`
- `documentation-sync`
- `git-branch-strategy`
- `release-branch-governance`
- `agent-handoff-protocol`
- `agent-swarm-coordination-specialist`
- `architecture-hexagonal`
- `architecture-modular-monorepo`
- `architecture-archunit-hexagonal`
- `microservice-senior-expert`
- `microservice-migration-safety-gate`
- `microservice-runtime-readiness-expert`
- `service-decomposition-bounded-context`
- `contract-governance-expert`
- `contract-first-api-steward`
- `data-ownership-persistence-steward`
- `resilience-engineering`
- `observability-runtime-diagnostics`
- `observability-diagnostics`
- `security-threat-modeling`
- `security-sandbox-specialist`
- `quality-gate`
- `quality-gate-governance`
- `quality-gate-orchestrator`
- `quality-testing-strategy`
- `testing-junit6`

## User-Requested Skill Names Needing Mapping

| Requested responsibility | Verified candidate |
| --- | --- |
| API Contract Governance Expert | `.agents/skills/contract-governance-expert/SKILL.md` and `.agents/skills/contract-first-api-steward/SKILL.md` |
| Service Boundary / Bounded Context Expert | `.agents/skills/service-decomposition-bounded-context/SKILL.md` |
| Resilience Engineering Expert | `.agents/skills/resilience-engineering/SKILL.md` |
| Observability / Tracing Expert | `.agents/skills/observability-runtime-diagnostics/SKILL.md` and `.agents/skills/observability-diagnostics/SKILL.md` |
| Security / Supply Chain Expert | `.agents/skills/security-threat-modeling/SKILL.md` and `.agents/skills/security-sandbox-specialist/SKILL.md` |
| Contract Testing | No exact standalone skill verified during workflow creation; Slice 08 must decide whether existing contract governance and testing skills cover it. |
| Migration Strategy | `.agents/skills/migration-workflow/SKILL.md` and `.agents/skills/microservice-migration-safety-gate/SKILL.md` |

## Prompt Path Finding

The user draft mentions `.codex/prompts/**`. The verified repository contains
`.agents/prompts/**`, and `.codex/AGENTS.md` says portable `.codex` files should
avoid project-specific governance. Slice execution must update `.agents/prompts`
by default unless a dedicated portability review authorizes creating
`.codex/prompts/**`.
