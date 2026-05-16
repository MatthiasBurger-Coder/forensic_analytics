# Workflow Execute Prompt - Skill And Agent Integrity Correction

Use this prompt when executing the active Skill and Agent Integrity Correction
workflow.

## Required Start

1. Read root `AGENTS.md`.
2. Read root `QUALITY.md`.
3. Read `docs/workflow/workflow.md` completely.
4. Read `docs/workflow/three-amigos-decision-record.md`.
5. Read `docs/workflow/agent-handoff-matrix.md`.
6. Read `.agents/orchestrator/routing-rules.md`.
7. Read `.agents/orchestrator/swarm-orchestrator.md`.
8. Verify the branch:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/feature/workflow-skill-agent-integrity-correction-20260516
git status --short --branch
```

Continue only when the active branch and local branch ref match the workflow
branch.

## Required Governance Gates

- Refresh Three Amigos readiness before mutating governance files.
- Run Skill Registry and Conflict Auditor before creating or changing skills.
- Route architecture authority changes through Senior System Architect review.
- Route microservice invariants through Microservice Senior Expert review.
- Route quality-gate changes through Senior Tester and Senior DevOps review.
- Use callable subagents only when explicitly authorized; otherwise use role
  files as local review checklists and report that limitation.

## Required Path Rules

- Use `.agents/skills/<skill-name>/SKILL.md` for skills.
- Use `.agents/prompts/**` for project prompts unless a portability review
  authorizes `.codex/prompts/**`.
- Do not create root `README.md` unless the responsible slice explicitly decides
  it is required. Existing `docs/README.md` is verified.

## Required Stop Conditions

Stop if:

- branch verification fails;
- Three Amigos is skipped;
- skill ownership is unclear;
- role hierarchy conflicts cannot be resolved;
- a slice would modify production code;
- a requested path conflicts with verified repository conventions;
- a quality command cannot be verified from `QUALITY.md` or build files;
- continuing would require guessing.
