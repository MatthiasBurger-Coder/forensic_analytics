# Agent Handoff Matrix

| Slice | Primary owner | Required reviewers | Write scope |
| --- | --- | --- | --- |
| 00 | Senior Workflow Architect | Senior DevOps Engineer, Senior Git Workspace Specialist when needed | `docs/workflow/execution-summary.md` |
| 01 | Senior Documentation Engineer | Senior Swarm Orchestrator, Skill Registry and Conflict Auditor | `docs/governance/skill-agent-inventory.md`, `docs/workflow/execution-summary.md` |
| 02 | Senior System Architect | Workflow / Workplan Executor, Three Amigos Gatekeeper, Senior Documentation Engineer | `AGENTS.md`, `docs/governance/agent-decision-chain.md` |
| 03 | Three Amigos Gatekeeper | Senior Tester, Senior System Architect, Senior Requirement Engineer | `.agents/skills/three-amigos-requirement-gatekeeper/**`, `.agents/prompts/workflow-create.md`, `docs/governance/three-amigos-gate.md` |
| 04 | Senior DevOps Engineer | Senior Workflow Architect, Senior Git Workspace Specialist | `.agents/skills/git-branch-strategy/SKILL.md`, `.agents/prompts/workflow-create.md`, `docs/governance/workflow-branching.md` |
| 05 | Senior Workflow Architect | Senior System Architect, Senior Tester | workflow executor/authoring skills, execution prompts, `docs/governance/workplan-slice-template.md` |
| 06 | Senior Swarm Orchestrator | Senior System Architect, Agent Handoff Protocol | swarm orchestrator role/skill/docs |
| 07 | Microservice Senior Expert | Senior System Architect, Senior DevOps Engineer, Senior gRPC/Proto Specialist, Contract Governance Expert | microservice expert role/skill/docs and verified ADR/arc42 references |
| 08 | Senior System Architect | Skill Registry and Conflict Auditor, Senior Swarm Orchestrator, Senior Documentation Engineer | `.agents/skills/**`, `docs/governance/skill-agent-inventory.md` |
| 09 | Senior Documentation Engineer | Senior System Architect, Senior Tester, Senior DevOps Engineer | `docs/governance/development-model-alignment.md`, optional `AGENTS.md` references |
| 10 | Senior Tester | Senior Workflow Architect, Senior Documentation Engineer | `docs/governance/workflow-traceability-matrix.md`, `.agents/prompts/workflow-create.md` |
| 11 | Senior Tester | Senior DevOps Engineer, Security / Supply Chain Expert | `QUALITY.md`, `docs/governance/governance-quality-gate.md` |
| 12 | Senior System Architect | Senior Documentation Engineer, Senior Workflow Architect, Microservice Senior Expert | `AGENTS.md` |
| 13 | Senior Tester | Senior System Architect, Senior Workflow Architect, Skill Registry and Conflict Auditor | `docs/governance/skill-agent-integrity-audit.md`, scoped follow-up fixes |
| 14 | Senior Workflow Architect | Senior DevOps Engineer, Senior Tester, git commit preparation skills | final execution notes and commit metadata only when authorized |

## Handoff Rules

- Every write-capable agent must verify the active branch before modifying
  files.
- Subagents must not switch branches.
- No implementation work may happen on `main`, `master`, `develop` or another
  shared branch.
- Each slice must record owner, acceptance criteria, affected files, expected
  tests, rollback notes and quality-gate command before implementation.
- A reviewer can block a slice when service ownership, contract ownership,
  quality commands or evidence semantics are unclear.
- Callable subagents are used only when explicitly authorized. Otherwise the
  matching role or skill file is used as a local review checklist.
- Handoffs must name the source slice, target slice, changed files, unresolved
  questions and verification evidence.
