# Workflow Execute Prompt

Use when the user writes `workflow execute`.

## Required Flow

1. Load root `AGENTS.md`.
2. Load root `QUALITY.md`.
3. Load the checked active workflow from `docs/workflow/workflow.md`.
4. Verify that `docs/workflow/workflow.md` records a checked arc42 review or update.
5. Load the checked or updated arc42 documentation from `docs/arc42/**`.
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
12. Stop and return to `workflow create` when the request introduces or changes scope.
13. Run Skill Conflict Audit before implementation slices.
14. Build or verify the slice plan.
15. Classify slices into backend, frontend, Docker/runtime and documentation strands.
16. Assign subagents or role reviews.
17. Use Agent Handoff Protocol for owner changes and parallel work.
18. Run slice quality gates and the final workflow execute gate.
19. Produce a summary with exact validation evidence.
20. Commit or push only when the workflow explicitly allows it and required gates are clean.

## Required Strand Checks

- Backend slices require Senior Java Backend Developer, Microservice Senior
  Expert when service boundaries are affected, `architecture-hexagonal`,
  `spring-core` when Spring wiring is affected, `testing-junit6` and Senior
  DevOps with `devops-docker` when container readiness is affected.
- Frontend slices require Senior React Frontend Developer, Senior UX Designer
  and Senior DevOps with `devops-docker` when container readiness is affected.
- Documentation slices must update execution report, test documentation, arc42
  consistency notes and deviations from `docs/workflow/workflow.md`.

## Stop Conditions

Stop when:

- active workflow cannot be identified;
- `docs/workflow/workflow.md` is missing or not checked;
- checked arc42 documentation is missing;
- active workflow branch is missing, inactive, or has no local ref;
- the request expands scope beyond `docs/workflow/workflow.md`;
- skill registry was skipped;
- requirement gate was required but skipped;
- subagent or role ownership is missing;
- handoff rules are missing for parallel work;
- required quality gates fail or cannot be verified;
- commit or push is requested without workflow permission.
