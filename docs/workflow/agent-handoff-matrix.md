# Agent Handoff Matrix

| Slice | Primary owner | Required reviewers | Write scope |
| --- | --- | --- | --- |
| 00 | Senior Workflow Architect | Senior System Architect, Senior Documentation Engineer, Senior Tester | `docs/workflow/**` |
| 01 | Senior Requirement Engineer | Skill Registry and Conflict Auditor, Senior System Architect, Senior Tester | `docs/workflow/**`, `docs/skill-audit/**` |
| 02 | Service Decomposition / Bounded Context Expert | Senior System Architect, Microservice Senior Expert, Senior Documentation Engineer | `.agents/skills/service-decomposition-bounded-context/**`, `docs/skill-audit/**` |
| 03 | Contract Governance Expert | Senior Java Backend Engineer, Senior gRPC/Proto Specialist, Senior System Architect | `.agents/skills/contract-governance-expert/**`, `docs/skill-audit/**` |
| 04 | Microservice Senior Expert | Senior System Architect, Senior Tester, Senior DevOps Engineer | `.agents/skills/microservice-migration-safety-gate/**`, `docs/skill-audit/**` |
| 05 | Senior DevOps Engineer | Microservice Senior Expert, Senior Tester | `.agents/skills/microservice-runtime-readiness-expert/**`, `docs/skill-audit/**` |
| 06 | Senior System Architect | Senior Workflow Architect, Senior Documentation Engineer, Microservice Senior Expert | `.agents/roles/senior-system-architect.md`, optional verified agent metadata, `AGENTS.md` |
| 07 | Senior Workflow Architect | Senior Tester, Senior DevOps Engineer, Senior System Architect | `.agents/prompts/**`, workflow authoring/executor skills, workflow architect role |
| 08 | Senior Requirement Engineer | Senior System Architect, Senior Tester, Microservice Senior Expert | Three Amigos skill, templates, decision rules and workflow-create prompt |
| 09 | Senior System Architect | Senior Documentation Engineer, Microservice Senior Expert, Contract Governance Expert | `AGENTS.md` |
| 10 | Senior Documentation Engineer | Senior System Architect, Senior DevOps Engineer, Microservice Senior Expert | `docs/architecture/**`, `docs/governance/**`, `docs/arc42/**`, `docs/adr/**` |
| 11 | Senior Tester | Senior DevOps Engineer, Senior Workflow Architect | `QUALITY.md`, existing README/docs/workplan links |
| 12 | Senior System Architect | Senior Workflow Architect, Senior Documentation Engineer, Microservice Senior Expert, Skill Registry and Conflict Auditor | changed governance and skill files |
| 13 | Senior Workflow Architect | Senior Tester, Senior System Architect, git commit preparation skills | final workflow notes and commit-preparation material |

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
