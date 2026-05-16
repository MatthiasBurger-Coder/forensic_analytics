# Microservice Skill Sharpening Execution Prompt

Use when the user runs `workflow execute` for the active Microservice Skill
Sharpening workflow.

## Required Reading

1. `AGENTS.md`
2. `QUALITY.md`
3. `.codex/AGENTS.md`
4. `.codex/workflow/workflow-execution-rules.md`
5. `.agents/orchestrator/routing-rules.md`
6. `.agents/orchestrator/swarm-orchestrator.md`
7. `.agents/skills/workflow-executor/SKILL.md`
8. `.agents/skills/workflow-authoring/SKILL.md`
9. `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md`
10. `.agents/skills/skill-registry-conflict-auditor/SKILL.md`
11. `.agents/skills/microservice-senior-expert/SKILL.md`
12. `docs/workflow/workflow.md`
13. `docs/workflow/skill-target-map.md`
14. `docs/workflow/microservice-governance-rules.md`
15. `docs/workflow/conflict-review.md`
16. `docs/workflow/quality-gate-plan.md`

## Execution Rules

- Verify the active branch is
  `feature/workflow-microservice-skill-sharpening-20260516`.
- Stop if unrelated or unclear uncommitted changes exist.
- Execute one slice at a time.
- Do not start implementation before the slice has a named owner, acceptance
  criteria, affected files, expected tests, rollback notes and quality-gate
  command.
- Use callable subagents or explicit role reviews according to repository
  availability and routing rules.
- Do not create service directories, contracts, deployment descriptors or
  production microservice code in this workflow.
- Do not introduce shared Java code modules.
- Do not claim a quality gate passed unless the exact command ran.

## Stop Report

Use this format when stopping:

```text
STOP: microservice skill sharpening cannot continue safely.
Reason: <concrete reason>
Inspected files:
- <file>
Expected:
- <expected item>
Found:
- <found item>
Why continuing would be unsafe:
- <reason>
```
