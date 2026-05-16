# Skill Target Map

The user draft listed several flat Markdown files under `.agents/skills`. The
verified repository convention is `.agents/skills/<skill-name>/SKILL.md` for
skills and `.agents/roles/<role-name>.md` for role descriptions.

Execution must not silently substitute paths. It must use the verified
repository convention unless the user explicitly overrides it during workflow
execution.

| Requested responsibility | User draft path | Verified execution target |
| --- | --- | --- |
| Service Decomposition / Bounded Context Expert | `.agents/skills/service-decomposition-bounded-context.md` | `.agents/skills/service-decomposition-bounded-context/SKILL.md` |
| Contract Governance Expert | `.agents/skills/contract-governance-expert.md` | `.agents/skills/contract-governance-expert/SKILL.md` |
| Microservice Migration Safety Gate | `.agents/skills/microservice-migration-safety-gate.md` | `.agents/skills/microservice-migration-safety-gate/SKILL.md` |
| Microservice Runtime Readiness Expert | `.agents/skills/microservice-runtime-readiness-expert.md` | `.agents/skills/microservice-runtime-readiness-expert/SKILL.md` |
| Senior System Architect | `.agents/skills/senior-system-architect.md` | `.agents/roles/senior-system-architect.md` and, if needed, `.codex/agents/senior_system_architect.toml` |
| Workplan / Workflow Executor | `.agents/skills/workplan-executor.md` | `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/workflow-authoring/SKILL.md`, `.agents/prompts/workflow-execute.md`, `.agents/prompts/slice-execute.md` |
| Three Amigos Requirements | `.agents/skills/three-amigos-requirements.md` | `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` and related templates/rules |
| Workflow create prompt | `.codex/prompts/workflow-create.md` | `.agents/prompts/workflow-create.md`; `.codex/prompts/**` is not present |
| Workplan execute prompt | `.codex/prompts/workplan-execute.md` | `.agents/prompts/workflow-execute.md` and `.agents/prompts/slice-execute.md`; `.codex/prompts/**` is not present |

## Stop Rule

If execution cannot verify the intended target path for a skill, role or prompt,
the slice must stop and report:

```text
STOP: microservice skill sharpening cannot continue safely.
Reason: target path for <responsibility> cannot be verified.
No skill or governance files were modified before resolving the path issue.
```
