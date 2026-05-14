# Agent Team Instructions

This directory contains the role, orchestration and repository-skill material for the Forensic Analytics development-team agent model.

The repository root `AGENTS.md` remains the authoritative source for engineering rules, evidence integrity, architecture boundaries, documentation language and stop conditions. `QUALITY.md` remains the authoritative source for local verification commands. If any file under `.agents/` conflicts with those root documents, the root document wins and the conflict must be reported before continuing.

## Operating Model

- Work in small, reviewable slices with explicit scope and verification.
- Preserve hexagonal architecture: adapters and infrastructure depend inward on application and domain, never the reverse.
- Keep forensic evidence semantics explicit; never convert unknown or incomplete facts into confirmed evidence.
- Reuse shared Codex skills instead of duplicating role-specific knowledge.
- Extend existing agent material only when the requested task requires it.
- Do not overwrite or rewrite existing repository-specific `SKILL.md` workflows unless the task explicitly targets them.

## Directory Map

- `orchestrator/` describes slice coordination, routing and conflict handling.
- `roles/` defines role responsibilities and required reference skills.
- `skills/<skill-name>/SKILL.md` contains repo-scoped Codex skills discovered by Codex.
- `../.codex/agents/` contains project-scoped custom subagent TOML files.

## Codex Compatibility

- A discoverable skill must be a directory under `.agents/skills/` with a `SKILL.md` file.
- Every `SKILL.md` must include YAML frontmatter with `name` and `description`.
- Custom subagents live under `.codex/agents/` as standalone TOML files with `name`, `description` and `developer_instructions`.

## Verification

For documentation-only changes, run the minimum meaningful repository verification first. For broader changes or before commit readiness, run the full local quality gate from `QUALITY.md`.
