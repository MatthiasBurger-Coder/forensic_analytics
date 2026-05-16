# Workflow Execute Prompt

Use when the user writes `workflow execute`.

## Required Flow

1. Load root `AGENTS.md`.
2. Load root `QUALITY.md`.
3. Locate the active workflow under `docs/workflow`.
4. Load `.agents/skills/workflow-executor/SKILL.md`.
5. Load `.agents/orchestrator/routing-rules.md`.
6. Load `.agents/orchestrator/swarm-orchestrator.md`.
7. Load Skill Registry & Conflict Auditor when available.
8. Run Requirement Gate when the request introduces or changes a requirement.
9. Run Skill Conflict Audit before implementation slices.
10. Build or verify the slice plan.
11. Assign subagents or role reviews.
12. Use Agent Handoff Protocol for owner changes and parallel work.
13. Run required quality gates.
14. Produce a summary with exact validation evidence.
15. Commit or push only when the workflow explicitly allows it and required gates are clean.

## Stop Conditions

Stop when:

- active workflow cannot be identified;
- skill registry was skipped;
- requirement gate was required but skipped;
- subagent or role ownership is missing;
- handoff rules are missing for parallel work;
- required quality gates fail or cannot be verified;
- commit or push is requested without workflow permission.
