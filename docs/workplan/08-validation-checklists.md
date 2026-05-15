# Validation Checklists

Use these checklists when creating, reviewing or executing governance work.

## EPIC Consistency

- Current EPIC source was identified.
- New requirement is traceable to EPIC, user request or verified implementation behavior.
- EPIC assumptions still match implementation.
- Requirement drift is recorded.
- Unresolved conflicts are documented.

## arc42 Consistency

- Relevant arc42 sections were reviewed.
- Architecture changes are reflected in the correct section.
- Runtime changes are documented in the runtime view when applicable.
- Deployment changes are documented in the deployment view when applicable.
- Crosscutting resilience, security, observability or evidence rules are updated when applicable.
- Planned behavior is not described as implemented behavior.

## Workplan Consistency

- `docs/workplan` was deleted before creating a new workplan.
- The full workplan structure was regenerated.
- No stale slices remain from previous workplans.
- Slices have stable numbering.
- Dependencies and parallelizable slices are explicit.
- Stop conditions are present.
- Verification commands are listed.

## Resilience Consistency

- New timeout, retry, circuit-breaker, bulkhead, health-check, cleanup or degraded-mode behavior is documented.
- Retry behavior is bounded and observable.
- Failure modes preserve evidence integrity.
- Resilience assumptions are reflected in arc42 and workplan slices.
- Tests or quality checks are planned where practical.

## Architecture Consistency

- Service ownership is explicit.
- Plugin versus server responsibility is explicit.
- Adapter, application and domain boundaries remain intact.
- No concrete provider dependency leaks into domain or application guidance.
- Architecture decisions are linked to ADRs where applicable.
- Architecture drift is not silently accepted.

## Service Boundary Consistency

- Inbound and outbound responsibilities are named.
- Communication protocols are documented.
- Runtime ownership is clear.
- Persistence ownership is clear.
- UI communication strategy is clear.
- Ambiguous ownership stops the work.

## Quality Gate Consistency

- `QUALITY.md` was reviewed.
- Gradle task names are verified before being documented.
- Minimum quality command is recorded.
- Full local quality gate is recorded when needed.
- `git diff --check` is run before commit readiness.
- Failed verification is reported with task, cause and blocker status.

## Skills And Roles Consistency

- New skills have YAML frontmatter.
- New skills live under `.agents/skills/<name>/SKILL.md`.
- New roles reference reusable skills instead of duplicating all rules.
- Existing roles are changed only when the reference is obvious and verified.
- Root `AGENTS.md` and `QUALITY.md` remain authoritative.
