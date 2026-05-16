# Prompt: Git Branch Strategy Workflow Execution

Use this prompt only after the active user command authorizes workflow
execution.

```text
Execute the active Git Branch Strategy workflow from docs/workflow/workflow.md.

Before implementation:
1. Verify the repository root, active branch and clean status from WSL.
2. Inspect feature/workflow-branch-isolation-20260516 and decide whether its
   branch-first workflow creation changes are a prerequisite, dependency or
   conflict.
3. Read AGENTS.md, QUALITY.md, .codex/workflow/workflow-execution-rules.md,
   .agents/orchestrator/routing-rules.md, .agents/orchestrator/swarm-orchestrator.md
   and the affected skills before editing.
4. Execute slices in order.
5. After each slice, run the slice checks, inspect git diff and update
   docs/workflow/execution-summary.md.
6. Stop instead of guessing branch ownership, task names, prompt ownership,
   Gradle tasks or governance authority.

At commit readiness:
1. Run the full local quality gate from QUALITY.md.
2. Verify git diff --check.
3. Commit only the approved workflow scope.
4. Push explicitly with git push -u origin feature/workflow-git-branch-strategy-20260516.
```
