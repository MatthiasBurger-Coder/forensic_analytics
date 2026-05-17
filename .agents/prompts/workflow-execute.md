# Workflow Execute Prompt

Use when the user writes `workflow execute`.

## Required Flow

1. Load root `AGENTS.md`.
2. Load root `QUALITY.md`.
3. Locate the active workflow under `docs/workflow`.
4. Verify that the active workflow includes a checked `docs/workflow/workflow.md`.
5. Verify that checked or updated `docs/arc42/**` documentation exists for the workflow.
6. Load `.agents/skills/workflow-executor/SKILL.md`.
7. Load `.agents/orchestrator/routing-rules.md`.
8. Load `.agents/orchestrator/swarm-orchestrator.md`.
9. Verify the active workflow branch before implementation:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/<workflow-branch>
git status --short --branch
```

10. Continue only when the active branch matches the workflow branch recorded by
   the active workflow and the local branch ref exists. If the branch must be
   created or restored, do so only after explicit user approval and rerun the
   ref and active-branch checks before file changes.
11. Load Skill Registry & Conflict Auditor when available.
12. Run Requirement Gate when the request introduces or changes a requirement.
13. Run Skill Conflict Audit before implementation slices.
14. Build or verify the slice plan.
15. Assign subagents or role reviews.
16. Use Agent Handoff Protocol for owner changes and parallel work.
17. Run required quality gates.
18. After each successful slice, run the slice checkpoint push defined by `docs/process/workflow-execute.md`.
19. Produce a summary with exact validation evidence.
20. Commit or push only when the workflow explicitly allows it and required gates are clean.

## Stop Conditions

Stop when:

- active workflow cannot be identified;
- checked `docs/workflow/workflow.md` is missing;
- checked or updated arc42 documentation is missing;
- active workflow branch is missing, inactive, or has no local ref;
- skill registry was skipped;
- requirement gate was required but skipped;
- subagent or role ownership is missing;
- handoff rules are missing for parallel work;
- required quality gates fail or cannot be verified;
- commit or push is requested without workflow permission.
- slice checkpoint push would include files outside the current slice;
- slice checkpoint push would push to `main`, create or merge a PR, run `push auto`, run branch cleanup or force-push.
