# Workflow Execute Prompt

Use when the user writes `workflow execute`.

## Required Flow

1. Load root `AGENTS.md`.
2. Load root `QUALITY.md`.
3. Locate the active workflow under `docs/workflow`.
4. Load `.agents/skills/workflow-executor/SKILL.md`.
5. Load `.agents/orchestrator/routing-rules.md`.
6. Load `.agents/orchestrator/swarm-orchestrator.md`.
7. Verify the active workflow branch before implementation:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/<workflow-branch>
git status --short --branch
```

8. Continue only when the active branch matches the workflow branch recorded by
   the active workflow and the local branch ref exists. If the branch must be
   created or restored, do so only after explicit user approval and rerun the
   ref and active-branch checks before file changes.
9. Load Skill Registry & Conflict Auditor when available.
10. Run Requirement Gate when the request introduces or changes a requirement.
11. Run Skill Conflict Audit before implementation slices.
12. Build or verify the slice plan.
13. Assign subagents or role reviews.
14. Use Agent Handoff Protocol for owner changes and parallel work.
15. Run required quality gates.
16. Produce a summary with exact validation evidence.
17. Commit or push only when the workflow explicitly allows it and required gates are clean.

## Stop Conditions

Stop when:

- active workflow cannot be identified;
- active workflow branch is missing, inactive, or has no local ref;
- skill registry was skipped;
- requirement gate was required but skipped;
- subagent or role ownership is missing;
- handoff rules are missing for parallel work;
- required quality gates fail or cannot be verified;
- commit or push is requested without workflow permission.
